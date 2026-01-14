C:\Users\808th\OneDrive\Desktop\A try> curl.exe -I http://mortadha.me
HTTP/1.1 200 OK
Server: nginx/1.24.0 (Ubuntu)
Date: Tue, 13 Jan 2026 20:56:27 GMT
Content-Type: text/html
Content-Length: 789
Last-Modified: Tue, 13 Jan 2026 21:43:08 GMT
Connection: keep-alive
ETag: "6966bc6c-315"
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Accept-Ranges: bytes



Visit http://mortadha.me (Ensure http:// and NO port). : both show the wildfly welcome message and page


root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~# sudo ss -tlnp | grep :80
LISTEN 0      4096         0.0.0.0:8080      0.0.0.0:*    users:(("java",pid=951210,fd=558))
LISTEN 0      511          0.0.0.0:80        0.0.0.0:*    users:(("nginx",pid=952806,fd=5),("nginx",pid=952805,fd=5))
LISTEN 0      511             [::]:80           [::]:*    users:(("nginx",pid=952806,fd=6),("nginx",pid=952805,fd=6))



root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~# tail -f /var/log/nginx/access.log
172.68.119.93 - - [13/Jan/2026:20:39:40 +0000] "GET /wp-includes/html-api/index.php HTTP/1.1" 200 445 "https://www.google.fr/" "Mozilla/5.0 (Linux; Android 11; CPH2251) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
172.68.119.93 - - [13/Jan/2026:20:39:40 +0000] "GET /wp-includes/pomo/index.php HTTP/1.1" 200 445 "https://www.yahoo.com/" "Mozilla/5.0 (Linux; Android 12; 2201116SG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
172.68.119.93 - - [13/Jan/2026:20:39:40 +0000] "GET /wp-includes/style-engine/index.php HTTP/1.1" 200 445 "https://www.yahoo.com/" "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
104.22.17.213 - - [13/Jan/2026:20:39:53 +0000] "GET / HTTP/1.1" 200 445 "-" "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1"
66.228.53.157 - - [13/Jan/2026:20:40:34 +0000] "GET / HTTP/1.1" 200 445 "-" "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
41.230.151.122 - - [13/Jan/2026:20:42:02 +0000] "HEAD / HTTP/1.1" 200 0 "-" "curl/8.16.0"
172.71.159.96 - - [13/Jan/2026:20:55:27 +0000] "GET / HTTP/1.1" 200 462 "-" "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1"
66.249.73.237 - - [13/Jan/2026:20:55:51 +0000] "GET /robots.txt HTTP/1.1" 200 462 "-" "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
66.249.73.238 - - [13/Jan/2026:20:55:51 +0000] "GET / HTTP/1.1" 200 462 "-" "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.7499.192 Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
41.230.151.122 - - [13/Jan/2026:20:56:27 +0000] "HEAD / HTTP/1.1" 200 0 "-" "curl/8.16.0"
*





root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~# cat /etc/nginx/sites-enabled/mortadha.me
# HTTP Server - Redirects to HTTPS (will be updated after SSL setup)
server {
    listen 80;
    listen [::]:80;
    server_name mortadha.me www.mortadha.me;
    # For Let's Encrypt verification
    location /.well-known/acme-challenge/ {
        root /var/www/mortadha.me;
    }
    # Redirect all HTTP to HTTPS (enable after SSL setup)
    # return 301 https://$server_name$request_uri;
    # Root and index
    root /var/www/mortadha.me;
    index index.html;
    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    # Frontend SPA
    location / {
        try_files $uri $uri/ /index.html;
    }
    # IAM Service API
    location /iam-service/ {
        proxy_pass http://127.0.0.1:8080/iam-service/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        # CORS Headers
        add_header 'Access-Control-Allow-Origin' '$http_origin' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization, X-Requested-With' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Max-Age' 86400;
            add_header 'Content-Length' 0;
            return 204;
        }
    }
    # API Gateway
    location /api-gateway/ {
        proxy_pass http://127.0.0.1:8080/api-gateway/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        # CORS Headers
        add_header 'Access-Control-Allow-Origin' '$http_origin' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization, X-Requested-With' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Max-Age' 86400;
            add_header 'Content-Length' 0;
            return 204;
        }
    }
    # Stego Module
    location /stego-module/ {
        proxy_pass http://127.0.0.1:8080/stego-module/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # For file uploads
        client_max_body_size 50M;
        # CORS Headers
        add_header 'Access-Control-Allow-Origin' '$http_origin' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization, X-Requested-With' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Max-Age' 86400;
            add_header 'Content-Length' 0;
            return 204;
        }
    }
}





root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~# curl http://localhost:8080/iam-service/api/health
{"status":"UP","service":"iam-service","version":"1.0.0","uptimeSeconds":1754,"timestamp":"2026-01-13T20:59:51.632246237Z"}root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~#




oot@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~# curl http://mortadha.me/iam-service/api/health
{"status":"UP","service":"iam-service","version":"1.0.0","uptimeSeconds":1768,"timestamp":"2026-01-13T21:00:05.653025014Z"}root@ubuntu-s-1vcpu-2gb-70gb-intel-fra1-01:~#


