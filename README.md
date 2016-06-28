Location Service with Spray
===========================

Description:
Simple REST location service using Spray and Google Maps API.

The service accepts only Post requests with Json payload as follows:

Request:
{
  "address": "leidseplein 25, amsterdam"
}

Response:
{
  "location": {
    "lat": 52.3635367,
    "lng": 4.882453
  }
}

How to run the server:

- `sbt run` => Starts the server at localhost:9090

How to change the default host or port:

- This can be done by changing the default values for the host and port in the application.conf file.
