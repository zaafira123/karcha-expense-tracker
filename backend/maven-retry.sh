#!/bin/sh
set -u

attempt=1
max_attempts=5
log_file=/tmp/maven-command.log

while [ "$attempt" -le "$max_attempts" ]; do
    if "$@" >"$log_file" 2>&1; then
        cat "$log_file"
        rm -f "$log_file"
        exit 0
    else
        status=$?
    fi

    cat "$log_file" >&2
    if ! grep -Eq '429|Too Many Requests' "$log_file" || [ "$attempt" -eq "$max_attempts" ]; then
        rm -f "$log_file"
        exit "$status"
    fi

    delay=$((attempt * 15))
    echo "Maven Central rate-limited the request; retrying in ${delay}s (attempt ${attempt}/${max_attempts})." >&2
    sleep "$delay"
    attempt=$((attempt + 1))
done

exit 1
