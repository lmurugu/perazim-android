const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const safety = require('./test_database_safety');
const dbUrl = process.env.DATABASE_URL || 'postgres://perazim_test:test_secret_pass_2026@127.0.0.1:5433/perazim_test';
safety.assertDisposableTestDatabase(dbUrl);
process.env.DATABASE_URL = dbUrl;
process.env.JWT_SECRET = 'browser-call-test-only-secret-2026';
process.env.PORT = process.env.PORT || '0';
const { startServer } = require('../src/index');
const { closeDb, query } = require('../src/db');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
let backend, staticServer, browser, api, testClassId, testGroupId, testUserIds=[];

async function call(path, token, method='GET', body) {
  const r = await fetch(api + path, { method, headers: { ...(token ? { authorization: `Bearer ${token}` } : {}), ...(body && method!=='GET' ? {'content-type':'application/json'} : {}) }, body: body && method!=='GET' ? JSON.stringify(body) : undefined });
  let data = {}; try { data = await r.json(); } catch {}
  return { status: r.status, body: data };
}
async function main() {
  backend = await startServer();
  api = `http://127.0.0.1:${backend.address().port}`;
  let phone = 1;
  async function member(name) {
    const number = `010${String(Date.now()).slice(-5)}${String(phone++).padStart(2,'0')}`;
    const r = await call('/api/auth/register', null, 'POST', { phone: number, pin: '7462', name, campusId: 'campus_embu' });
    assert.equal(r.status, 201, JSON.stringify(r.body));
    return r.body;
  }
  const teacher = await member('Synthetic Video Teacher');
  const learner = await member('Synthetic Video Learner');
  const outsider = await member('Synthetic Video Outsider');
  testUserIds=[teacher.user.id,learner.user.id,outsider.user.id];
  const group = await call('/api/community/groups', teacher.token, 'POST', { id:`browser_group_${Date.now()}`, name:'Synthetic private video test' });
  assert.equal(group.status, 201, JSON.stringify(group.body));
  testGroupId=group.body.group.id;
  assert.equal((await call(`/api/community/groups/${group.body.group.id}/members`, teacher.token, 'POST', {userId:learner.user.id})).status, 200);
  const classId = `browser_class_${Date.now()}`;
  testClassId=classId;
  const created = await call('/api/community/classes', teacher.token, 'POST', {id:classId, groupId:group.body.group.id, title:'Synthetic WebRTC test', startsAt:Date.now()+3600000, durationMinutes:30});
  assert.equal(created.status, 201, JSON.stringify(created.body));
  assert.equal((await call(`/api/classroom-calls/${classId}/join`, outsider.token, 'POST', {joinKey:'abcdefghijklmnop'})).status, 403, 'outsider should be blocked from class call');
  console.log('PASS private class access rejects outsider (403)');

  const html = fs.readFileSync(path.resolve(__dirname, '../../src/main/assets/classroom.html'));
  staticServer = http.createServer((req,res)=>{res.writeHead(200,{'content-type':'text/html; charset=utf-8','cache-control':'no-store'});res.end(html);});
  await new Promise(resolve=>staticServer.listen(0,'127.0.0.1',resolve));
  const pageUrl=`http://127.0.0.1:${staticServer.address().port}`;
  browser = await chromium.launch({headless:true, executablePath:process.env.CHROMIUM_PATH, args:['--no-sandbox','--use-fake-ui-for-media-stream','--use-fake-device-for-media-stream']});
  const pages=[]; const bridges=[];
  async function newPeer(identity) {
    const context=await browser.newContext({permissions:['camera','microphone']});
    const page=await context.newPage(); const counters={signals:[],events:[],session:null};
    page.on('pageerror',e=>console.error('PAGEERROR',identity.user.name,e.message)); page.on('console',m=>{if(m.type()==='error')console.error('CONSOLE',identity.user.name,m.text());});
    await page.exposeFunction('__bridgeRequest',async(id,action,raw)=>{
      const body=JSON.parse(raw||'{}'); let method='GET',path;
      if(action==='join'){method='POST';path=`/api/classroom-calls/${classId}/join`;}
      else if(action==='poll'){path=`/api/classroom-calls/${classId}/${counters.session}/events?after=${encodeURIComponent(body.after||0)}`;}
      else if(action==='signal'){method='POST';path=`/api/classroom-calls/${classId}/${counters.session}/signal`;counters.signals.push(body.payload.type);}
      else throw new Error(`unexpected bridge action ${action}`);
      const result=await call(path,identity.token,method,body);
      if(action==='join' && result.status===200)counters.session=result.body.sessionId;
      if(action==='poll' && result.status===200)counters.events.push(...result.body.events.map(e=>e.payload.type));
      await page.evaluate(([rid,ok,res])=>window.nativeResponse(rid,ok,res),[id,result.status>=200&&result.status<300,result.body]);
    });
    await page.addInitScript(()=>{window.ClassroomNative={request:(id,action,body)=>window.__bridgeRequest(id,action,body),close:()=>{}};});
    await page.goto(pageUrl);
    pages.push({page,context}); bridges.push(counters); return {page,counters,context};
  }
  const a=await newPeer(teacher), b=await newPeer(learner);
  for(const p of [a,b]) await p.page.locator('#join').click();
  try { await a.page.waitForFunction(()=>document.querySelectorAll('#videos video').length===2, {timeout:10000}); }
  catch(e) { console.error('peer snapshots', await Promise.all([a,b].map(async p=>({status:await p.page.locator('#status').textContent(), tiles:await p.page.locator('#videos').innerHTML(), counters:p.counters})))); throw e; }
  try { await b.page.waitForFunction(()=>document.querySelectorAll('#videos video').length===2, {timeout:10000}); }
  catch(e) { console.error('peer snapshots', await Promise.all([a,b].map(async p=>({status:await p.page.locator('#status').textContent(), tiles:await p.page.locator('#videos').innerHTML(), counters:p.counters})))); throw e; }
  await a.page.waitForFunction(()=>document.querySelector('#tile-local')?.querySelector('video')?.srcObject?.getVideoTracks()?.[0]?.readyState==='live');
  await b.page.waitForFunction(()=>document.querySelector('#tile-local')?.querySelector('video')?.srcObject?.getAudioTracks()?.[0]?.readyState==='live');
  await a.page.waitForFunction(()=>{const v=[...document.querySelectorAll('#videos video')].find(x=>!x.muted);return !!v&&v.readyState>=2&&v.videoWidth>0&&v.videoHeight>0;},{timeout:30000});
  const videoState=await a.page.evaluate(async()=>{const v=[...document.querySelectorAll('#videos video')].find(x=>!x.muted);const reports=await [...peers.values()][0].pc.getStats();const inbound=[...reports.values()].filter(r=>r.type==='inbound-rtp').map(r=>({kind:r.kind||r.mediaType,bytesReceived:r.bytesReceived,framesDecoded:r.framesDecoded,packetsReceived:r.packetsReceived}));return {width:v.videoWidth,height:v.videoHeight,inbound};});
  assert(videoState.width>0&&videoState.height>0&&videoState.inbound.some(r=>r.kind==='video'&&(r.framesDecoded>0||r.bytesReceived>0)),JSON.stringify(videoState));
  const events=[...a.counters.events,...b.counters.events,...a.counters.signals,...b.counters.signals];
  assert(events.includes('offer')&&events.includes('answer')&&events.includes('ice'),`signaling events missing: ${JSON.stringify(events)}`);
  console.log(`PASS two authenticated original classroom.html peers negotiated offer/answer/ICE; remote video ${videoState.width}x${videoState.height}, inbound RTP ${JSON.stringify(videoState.inbound)}`);
  const localChecks=await a.page.evaluate(()=>{
    const audio=document.querySelector('#tile-local video').srcObject.getAudioTracks()[0];
    document.querySelector('#mic').click(); const muted=!audio.enabled;
    document.querySelector('#mic').click(); const unmuted=audio.enabled;
    const video=document.querySelector('#tile-local video').srcObject.getVideoTracks()[0];
    document.querySelector('#camera').click(); const cameraOff=!video.enabled;
    document.querySelector('#camera').click(); return {muted,unmuted,cameraOff,cameraOn:video.enabled};
  });
  assert.deepEqual(localChecks,{muted:true,unmuted:true,cameraOff:true,cameraOn:true});
  await a.page.locator('#leave').click();
  assert.equal((await call(`/api/classroom-calls/${classId}/${a.counters.session}`,teacher.token,'DELETE')).status,200);
  const bPoll=await call(`/api/classroom-calls/${classId}/${b.counters.session}/events?after=0`,learner.token);
  assert.equal(bPoll.status,200);assert.equal(bPoll.body.participants.length,1);
  console.log('PASS mute/unmute, camera on/off, local Leave UI, and server session removal');
  for(const p of pages) await p.context.close();
}
main().catch(e=>{console.error('FAIL',e.stack||e);process.exitCode=1;}).finally(async()=>{
  if(browser)await browser.close();
  if(staticServer)await new Promise(r=>staticServer.close(r));
  if(testClassId)await query('DELETE FROM community_classes WHERE id = $1',[testClassId]).catch(()=>{});
  if(testGroupId){await query('DELETE FROM group_memberships WHERE group_id = $1',[testGroupId]).catch(()=>{});await query('DELETE FROM community_groups WHERE id = $1',[testGroupId]).catch(()=>{});}
  if(testUserIds.length)await query('DELETE FROM users WHERE id = ANY($1::text[])',[testUserIds]).catch(()=>{});
  if(backend)await new Promise(r=>backend.close(r)); await closeDb();
});
