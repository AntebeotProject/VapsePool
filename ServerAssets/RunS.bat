color 96
Powershell.exe -executionpolicy remotesigned -File C:\poolserver\ver3\start_coins.ps1
timeout 90
java -cp Antibiotic.jar org.antibiotic.pool.main.MainKt
pause