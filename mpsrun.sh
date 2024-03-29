#!/bin/bash


start() {
  echo "Starting containers..."
  docker-compose up --build -d

}
# start containers in development mode
start_dev() {
    echo "Starting in development mode..."
    # remove volume for fresh start
    docker volume rm mproc-swift_db-data
    # remove docker-compose cache
    docker-compose down --volumes --remove-orphans
    # rebuild and start
    docker-compose up --build -d
}

stop_containers() {
    echo "Stopping containers..."
    docker-compose down
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
    *)
        echo "Usage: mpsrun {start|start --dev|stop}"
        exit 1
esac
