CREATE USER payments_app WITH PASSWORD 'app_password';

GRANT CONNECT ON DATABASE payments TO payments_app;