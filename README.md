# javaproject
whatsapp webhook bot, simple oauth2 login flow

deployed on gcp app engine to serve the whatsapp webhook bot at url "/webhook"

project folders in use are /config and /controllers (others were temporary test which are not currently in use, they are also commented out)

1. Whatsapp Webhook bot
- uses google cloud platform, mongodb
- uses Facebook Business and Meta for Developers accounts
- a test whatsapp number provided by meta for developers to which you can send messages to and the bot will respond with automation
- the automation consists of consuming and processing text, image and location type messages (all other types have been captured and stored in application state without further processing)
- the flow steps are:
  - user sends text message from his phone to test whatsapp number - message with all message details are stored in a cloud mongodb cluster database
  - user sends an image message - message details are stored in mongodb, meanwhile a google cloud function gets triggered via an http call where the image is being downloaded via facebook.graph's given url for downloading media in binary mode, stored in google cloud storage bucket, and public url is being appended back in mongodb to the original message
  - user sends a location message - message details (including lat/lng coordinates) are being stored in mongodb
  - user sends a text message starting with the words "Bearing" and included with a static distance of for example "100m S" (100 meters South, or any other valid bearing) then the webhook triggers another google cloud function via http triggers which runs a calculation formula based on the latest location message from the same user which calculates new coordinates of for example 100 meters South from the original lat/lng coordinates sent in the first message then stores both pairs in a new mongodb collection "map" dedicated for map rendering purposes.
  

2. OAuth login flow at url "/" (works on localhost:8080) deployed urls haven't been configured to have access from google cloud console
