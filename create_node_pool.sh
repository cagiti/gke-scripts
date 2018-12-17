#!/bin/bash

CLUSTER=jenkins-x-infra-production
NAME=pool-3
DISK_SIZE=200
ZONE=europe-west1-b
MIN_NODES=3
MAX_NODES=10
MACHINE_TYPE=n1-standard-4

gcloud container node-pools create $NAME \
        --cluster=$CLUSTER \
        --disk-size=$DISK_SIZE \
        --enable-autorepair \
        --enable-autoupgrade \
        --enable-autoscaling \
        --max-nodes=$MAX_NODES \
        --min-nodes=$MIN_NODES \
        --machine-type=$MACHINE_TYPE \
        --num-nodes=$MIN_NODES \
        --zone=$ZONE \
        --preemptible \
        --scopes=https://www.googleapis.com/auth/compute,https://www.googleapis.com/auth/devstorage.full_control,https://www.googleapis.com/auth/service.management,https://www.googleapis.com/auth/servicecontrol,https://www.googleapis.com/auth/logging.write,https://www.googleapis.com/auth/monitoring,https://www.googleapis.com/auth/cloud-platform


