#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "用法: $0 <appId> <port> [nginx.conf路径,默认:/etc/nginx/nginx.conf]"
  exit 1
fi

appId="$1"
port="$2"
nginxConfigPath="/etc/nginx/nginx.conf"

if [[ ! -f "${nginxConfigPath}" ]]; then
  echo "找不到 nginx 配置文件: ${nginxConfigPath}"
  exit 1
fi

tmpFile="$(mktemp "${nginxConfigPath}.XXXX")"

awk -v appId="${appId}" -v port="${port}" '
function countChar(str, ch,    i, cnt) {
  cnt = 0;
  for (i = 1; i <= length(str); i++) {
    if (substr(str, i, 1) == ch) cnt++;
  }
  return cnt;
}
function printLocation() {
  print "         location /" appId "/api/ {";
  print "             proxy_pass http://localhost:" port "/api/;";
  print "             proxy_http_version 1.1;";
  print "             proxy_set_header Connection \"\";";
  print "             proxy_set_header Host $http_host;";
  print "             proxy_set_header X-Real-IP $remote_addr;";
  print "             proxy_set_header REMOTE-HOST $remote_addr;";
  print "             proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;";
  print "             proxy_connect_timeout    300s;";
  print "             proxy_read_timeout       300s;";
  print "             proxy_send_timeout       300s;";
  print "        }";
}
BEGIN {
  inServer = 0;
  braceCount = 0;
  skipBlock = 0;
  skipDepth = 0;
}

/^[[:space:]]*server[[:space:]]*{/ {
  inServer = 1;
  braceCount = 1;
  print;
  next;
}

{
  if (skipBlock) {
    skipDepth += countChar($0, "{");
    skipDepth -= countChar($0, "}");
    if (skipDepth <= 0) {
      skipBlock = 0;
    }
    next;
  }

  if ($0 ~ ("location[[:space:]]+/" appId "/api/[[:space:]]*{")) {
    skipBlock = 1;
    skipDepth = countChar($0, "{") - countChar($0, "}");
    if (skipDepth <= 0) {
      skipBlock = 0;
    }
    next;
  }

  if (inServer && braceCount == 1 && $0 ~ /^[[:space:]]*}/) {
    printLocation();
    inServer = 0;
  }

  print;

  if (inServer) {
    braceCount += countChar($0, "{");
    braceCount -= countChar($0, "}");
    if (braceCount == 0) {
      inServer = 0;
    }
  }
}
' "${nginxConfigPath}" > "${tmpFile}"

cat "${tmpFile}" > "${nginxConfigPath}"
rm -f "${tmpFile}"