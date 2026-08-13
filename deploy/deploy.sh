#!/bin/bash
# 内部培训平台 · 部署脚本（Linux）
#
# 用法：
#   ./deploy.sh init      首次部署：建目录、装 systemd 单元
#   ./deploy.sh app       更新后端：上传 jar 后重启 4 个 Java 服务
#   ./deploy.sh web       更新前端：部署 admin 静态文件 + 门户 SSR 并重启
#   ./deploy.sh status    查看所有服务状态
#   ./deploy.sh logs      跟踪日志
#
# 前提：jar 与前端产物已上传到 /opt/training/upload/ 下
#   upload/app/*.jar         后端 4 个 jar
#   upload/admin/            后台打包产物（admin 的 dist/ 内容）
#   upload/web/              门户打包产物（web 的 .output/ 内容）

set -euo pipefail

BASE=/opt/training
UPLOAD=$BASE/upload
SERVICES=(training-system training-user training-course training-gateway)
ALL=("${SERVICES[@]}" training-web)

log() { echo -e "\033[32m[$(date '+%H:%M:%S')]\033[0m $*"; }
err() { echo -e "\033[31m[错误]\033[0m $*" >&2; }

need_root() {
  if [ "$(id -u)" -ne 0 ]; then
    err "请用 root 运行"
    exit 1
  fi
}

case "${1:-}" in

  init)
    need_root
    log "创建目录"
    mkdir -p $BASE/{app,conf,logs,admin,web,upload}
    # 数据盘：视频存这里，务必单独挂载，不要放系统盘
    mkdir -p /data/uploads/{public,private}

    if ! id training &>/dev/null; then
      log "创建 training 用户"
      useradd -r -s /sbin/nologin training
    fi

    log "安装 systemd 单元"
    cp systemd/*.service /etc/systemd/system/
    systemctl daemon-reload

    if [ ! -f $BASE/conf/training.env ]; then
      cp training.env.example $BASE/conf/training.env
      chmod 600 $BASE/conf/training.env
      err "请先编辑 $BASE/conf/training.env 填入数据库密码、JWT 密钥、文件签名密钥，再执行 ./deploy.sh app"
    fi

    chown -R training:training $BASE /data/uploads
    log "初始化完成"
    ;;

  app)
    need_root
    if ! ls $UPLOAD/app/*.jar >/dev/null 2>&1; then
      err "$UPLOAD/app/ 下没有 jar 文件"
      exit 1
    fi

    log "停止后端服务"
    for s in "${SERVICES[@]}"; do systemctl stop $s 2>/dev/null || true; done

    log "备份当前版本"
    ts=$(date +%Y%m%d%H%M%S)
    if ls $BASE/app/*.jar >/dev/null 2>&1; then
      mkdir -p $BASE/backup/$ts && cp $BASE/app/*.jar $BASE/backup/$ts/
      log "已备份到 $BASE/backup/$ts（回滚时拷回 $BASE/app/ 再重启即可）"
    fi

    log "部署新版本"
    cp $UPLOAD/app/*.jar $BASE/app/
    chown -R training:training $BASE/app

    log "启动后端服务"
    for s in "${SERVICES[@]}"; do systemctl enable --now $s; done

    sleep 20
    $0 status
    ;;

  web)
    need_root
    log "部署管理后台静态文件"
    rm -rf $BASE/admin/* && cp -r $UPLOAD/admin/* $BASE/admin/

    log "部署门户 SSR"
    systemctl stop training-web 2>/dev/null || true
    rm -rf $BASE/web/* && cp -r $UPLOAD/web/* $BASE/web/
    chown -R training:training $BASE/admin $BASE/web

    systemctl enable --now training-web
    nginx -t && systemctl reload nginx
    log "前端部署完成"
    ;;

  status)
    printf "%-22s %-10s %s\n" 服务 状态 端口
    printf -- "-%.0s" {1..50}; echo
    for s in "${ALL[@]}"; do
      st=$(systemctl is-active $s 2>/dev/null || echo unknown)
      printf "%-22s %-10s\n" "$s" "$st"
    done
    echo
    echo "端口监听："
    ss -lntp 2>/dev/null | grep -E ':(3000|7700|7710|7720|7730)\b' || echo "  （无）"
    ;;

  logs)
    journalctl -u 'training-*' -f
    ;;

  *)
    grep '^#' "$0" | head -18 | sed 's/^# \?//'
    exit 1
    ;;
esac
