# Log-monitor microservice (lives in the ioc-building mono repo): tails the
# ioc-building log file from a shared volume and posts bug reports to the
# code-review agent's webhook.
#   docker build -t log-monitor ./log-monitor
FROM python:3.12-alpine
WORKDIR /app
COPY log_monitor.py .
# Unbuffered stdout so `docker logs log-monitor` shows activity immediately.
ENV PYTHONUNBUFFERED=1 \
    LOG_FILE=/logs/app.log \
    WEBHOOK_URL=http://code-review:8090/webhook/bugreport \
    SERVICE_NAME=ioc-building
# The log file only exists once ioc-building has written its first line.
CMD ["sh", "-c", "until [ -f \"$LOG_FILE\" ]; do echo \"waiting for $LOG_FILE\"; sleep 2; done; exec python log_monitor.py --file \"$LOG_FILE\" --token \"$WEBHOOK_TOKEN\" --url \"$WEBHOOK_URL\" --service \"$SERVICE_NAME\""]
