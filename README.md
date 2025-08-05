<h1>Инструкция по запуску приложения</h1>

<h2>Генерация ключа</h2>
  <p>
    Для запуска приложения необходимо сгенерировать ключ шифрования с командой
    bash openssl rand -base64 32. 
    Полученный ключ нужно вставить в .env файл 
    (ENCRYPTION_KEY=*ключ*). В случае запуска через Intelliji IDEA нужно указать путь до этого файла в Environment Variables. 
  </p>
  
<h2>Запуск из IntelliJ IDEA</h2>
  <p>При запуске из IntelliJ IDEA необходимо включить в Active profiles профиль dev.
    Затем нужно поднять базу данных в docker-compose (docker-compose up -d db) и запустить приложение при помощи
    конфигурации BankcardsApplication.</p>

<h2>Запуск через Docker</h2>
  <p>Перед запуском через Docker необходимо собрать jar-файл (mvn package).</p>
  <p>Далее нужно поднять все сервисы в docker-compose (docker-compose up -d).</p>

