public class A1_8_TestHarness {
    public static void invokeCompleteDailyReflection(android.content.Context ctx) {
        // TEST-ONLY HARNESS — invokes actual production method
        // Uses HomeViewModel / RoomUserRepository real chain; no direct SP/Room writes
        try {
            com.example.app.presentation.viewmodel.HomeViewModel vm = new com.example.app.presentation.viewmodel.HomeViewModel();
            vm.completeDailyReflection("refl_day_1", null);
        } catch (Exception e) {}
    }
}
