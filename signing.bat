@echo off
call "%~dp0setup.bat" signing %*
exit /b %errorlevel%
