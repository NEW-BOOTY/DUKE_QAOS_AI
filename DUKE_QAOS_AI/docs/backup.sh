#!/bin/bash
# backup.sh - Production backup script

set -euo pipefail

BACKUP_DIR="/backups/dukeai/$(date +%Y%m%d)"
DB_BACKUP="$BACKUP_DIR/dukeai_db_$(date +%H%M%S).sql"
KEY_BACKUP="$BACKUP_DIR/keys_$(date +%H%M%S).tar.gz"

mkdir -p "$BACKUP_DIR"

# Database backup
pg_dump -h localhost -U dukeai_user duke_ai_production > "$DB_BACKUP"

# Key backup (encrypted)
tar -czf - /secure/keys/ | gpg --symmetric --cipher-algo AES256 -o "$KEY_BACKUP"

# Application state
java -cp /opt/dukeai/DUKEAi-1.0.0.jar \
  com.devinroyal.dukeai.utils.BackupUtility \
  --export-state "$BACKUP_DIR/state.json"

# Upload to secure storage
aws s3 cp "$BACKUP_DIR" s3://dukeai-backups/ --recursive --sse AES256

# Cleanup old backups (keep 7 days)
find /backups/dukeai -type d -mtime +7 -exec rm -rf {} +

echo "Backup completed: $BACKUP_DIR"