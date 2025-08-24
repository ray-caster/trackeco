@echo off
:: This batch script automates starting the Android emulator and installing the EcoTrack app.
:: It should be run from the root "trackeco" project directory.

ECHO #########################################
ECHO #         EcoTrack Quick-Start          #
ECHO #########################################
ECHO.

:: Step 1: Start the Android emulator in a new, separate window using the correct AVD name.
:: We use the 'START' command so this script can continue without waiting.
ECHO [1/4] Starting the Android Emulator (Medium_Phone_API_36.0)...
START "Android Emulator" emulator -avd Medium_Phone_API_36.0
ECHO.

:: Step 2: Wait for the emulator to fully boot up.
:: This is a critical step. We will wait for 45 seconds. If the install
:: fails with "no connected devices", increase this number to 60 or 90.
ECHO [2/4] Waiting for 45 seconds for the emulator to boot...
TIMEOUT /T 45
ECHO.

:: Step 3: Navigate into the Android project directory.
:: The gradlew command must be run from inside the 'android_app' folder.
ECHO [3/4] Changing directory to 'android'...
cd android
ECHO.

:: Step 4: Clean the previous build and install the new debug version of the app.
:: The 'call' command ensures that once gradlew is done, this script continues.
ECHO [4/4] Building and installing the EcoTrack app...
ECHO This might take a minute...
call .\gradlew.bat clean installDebug
ECHO.

ECHO #########################################
ECHO #              PROCESS COMPLETE              #
ECHO #########################################
ECHO.
ECHO The 'EcoTrack' app should now be installed on your emulator.
ECHO You can find it in the app drawer (swipe up on the home screen).
ECHO.
ECHO Press any key to close this window.
PAUSE