#!/bin/bash
# This script builds a Docker image for ApiPaymentProcessorThreads.
docker build -t api-payment-processor-threads:latest  .

# Check if the build was successful
if [ $? -eq 0 ]; then
    echo "Docker image 'api-payment-processor-threads:latest' built successfully."
else
    echo "Failed to build the Docker image."
    exit 1
fi

# If you want to push the image to a Docker registry, uncomment the following lines:
docker login

# Tag the image with your Docker Hub username or registry name
docker tag api-payment-processor-threads:latest maxsonferovante/api-payment-processor-threads:latest

# Push the image to the Docker registry
docker push maxsonferovante/api-payment-processor-threads:latest