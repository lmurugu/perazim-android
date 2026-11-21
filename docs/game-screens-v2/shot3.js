const { chromium } = require('playwright');
(async()=>{const b=await chromium.launch({executablePath:'/opt/pw-browsers/chromium',args:['--no-sandbox']});
const p=await b.newPage({viewport:{width:390,height:844},deviceScaleFactor:2});
for(const f of ['vine_words','lamp_slide','bible_quiz']) for(const m of ['light','dark']){
 await p.goto('file:///home/user/mock2/'+f+'_'+m+'.html'); await p.waitForLoadState('load'); await p.screenshot({path:`/mnt/project-files/game-screens-v2/v3_${f}_${m}.png`});}
await b.close();})();
