# HTTP -> HTTPS 跳转
server {
    listen 80;
    server_name d.ytcbd.com;

    location ^~ /.well-known/acme-challenge/ {
        root /www/wwwroot/down;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS 主配置
server {
    listen 443 ssl http2;
    server_name d.ytcbd.com;

    ssl_certificate /etc/letsencrypt/live/d.ytcbd.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/d.ytcbd.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    root /www/wwwroot/down;

    location ~* \.apk$ {
        types { application/vnd.android.package-archive apk; }
        add_header Content-Disposition 'attachment';
    }

    location / {
        try_files $uri $uri/ =404;
    }

    access_log /var/log/nginx/d_ytcbd.access.log;
    error_log /var/log/nginx/d_ytcbd.error.log;
}