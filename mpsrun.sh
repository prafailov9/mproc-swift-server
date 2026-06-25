#!/bin/bash

start() {
  echo "Starting containers..."
  docker compose up --build -d

}
# start containers in development mode
start_dev() {
       echo "Starting in development mode..."

        docker compose down --volumes --remove-orphans
        docker volume rm mproc-swift_db-data 2>/dev/null || true

        docker compose up --build -d
}

stop_containers() {
    echo "Stopping containers..."
    docker compose down
}

restart() {
  stop_containers
  if ! mvn clean install; then
    echo "Maven build failed. Skipping docker compose start."
    return 1
  fi
  start
}

restart_dev() {
  stop_containers
     if ! mvn clean install; then
        echo "Maven build failed. Skipping docker compose start in dev mode."
        return 1
      fi
    start_dev
}

# Main script logic
case "$1" in
    start)
        if [ -z "$2" ];
        then
          start

        elif [ "$2" == "--dev" ];
        then
            start_dev
        else
          echo "Invalid argument for start. Use --dev for development mode."
        fi
        ;;
    stop)
        stop_containers
        ;;
    restart)
      restart
      ;;
    restart_dev)
    restart_dev
    ;;
    *)
        echo "Usage: mpsrun {start|start --dev|stop}"
        exit 1
esac
