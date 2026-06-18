@echo off
echo ========================================
echo Building All Microservices
echo ========================================
echo.

set "SERVICES=eureka-server api-gateway auth-service company-service job-service resume-service file-service notification-service search-service"
set "FAILED_SERVICES="
set "SUCCESS_COUNT=0"
set "FAIL_COUNT=0"

for %%S in (%SERVICES%) do (
    echo.
    echo ========================================
    echo Building: %%S
    echo ========================================

    cd /d "%~dp0%%S"

    if exist "gradlew.bat" (
        call gradlew.bat clean build -x test

        if errorlevel 1 (
            echo [FAILED] %%S build failed!
            set "FAILED_SERVICES=!FAILED_SERVICES! %%S"
            set /a FAIL_COUNT+=1
        ) else (
            echo [SUCCESS] %%S built successfully!
            set /a SUCCESS_COUNT+=1
        )
    ) else (
        echo [ERROR] gradlew.bat not found in %%S
        set "FAILED_SERVICES=!FAILED_SERVICES! %%S"
        set /a FAIL_COUNT+=1
    )
)

cd /d "%~dp0"

echo.
echo ========================================
echo Build Summary
echo ========================================
echo Total Services: 9
echo Success: %SUCCESS_COUNT%
echo Failed: %FAIL_COUNT%

if not "%FAILED_SERVICES%"=="" (
    echo.
    echo Failed Services:%FAILED_SERVICES%
)

echo.
echo ========================================
echo Build Process Completed
echo ========================================
pause

