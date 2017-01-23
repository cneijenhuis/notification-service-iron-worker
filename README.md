## IronWorker Notification Service Example for Sunrise iOS App

[Sunrise iOS app](https://github.com/commercetools/commercetools-sunrise-ios) push notifications are triggered using Commercetools [subscriptions](http://dev.commercetools.com/http-api-projects-subscriptions.html). One of the possible [destinations](http://dev.commercetools.com/http-api-projects-subscriptions.html#destination) for a subscription is an [IronMQ](https://www.iron.io/platform/ironmq/). Messages published to an IronMQ can trigger [IronWorker](https://www.iron.io/platform/ironworker/) if subscribed.
This example will show you how you can easily get notifications to your mobile shop app running on [Commercetools](https://commercetools.com) platform, without a need for a server instance. The scope of this example covers _reservation notification_ scenario, triggered on `OrderCreated` [message](http://dev.commercetools.com/http-api-projects-messages.html#ordercreated-message).

### Setup

#### Credentials

In order to use this example you need:
- An active Commercetools project. Replace `PROJECT_KEY`, `SCOPE`, `CLIENT_ID`, `CLIENT_SECRET`, `API_URL` and `AUTH_URL` with values valid for your project.
- Docker Hub account for pushing the Docker image built from the `NotificationUtil.groovy` script.
- Active Iron.io account.

#### Build a JAR file

- Before building the JAR file containing the script and all other dependencies, you need to copy your `.p12` APNS certificate to `src/main/resources` directory. Make sure to replace `InputStream certificate = this.getClass().getResourceAsStream('/Sunrise.p12')` with your certificate name.
- Run `gradle fatJar` to build a JAR file which contains all dependencies used in the `NotificationUtil.groovy` script.
  - Optionally, you can use `gradle runScript` command to run the script locally for debugging purposes.
  
#### Create an IronWorker

- Create a `Dockerfile` in the same directory where your newly created `.jar` file is located.
```
FROM iron/java

WORKDIR /app
ADD . /app

ENTRYPOINT ["java", "-jar", "notification-service.jar"]
```
  - Make sure to replace `notification-service.jar` with the proper name of the JAR file you have built.
  
- Build your Docker image: `docker build -t USERNAME/IMAGENAME:0.0.1 .`, where _USERNAME_ and _IMAGENAME_ should match the name of your Docker hub username and the image name you want to assign to your notification service.
- Push it to Docker Hub: `docker push USERNAME/IMAGENAME:0.0.1`.
- Register your image with Iron: `iron register USERNAME/IMAGENAME:0.0.1`.

#### Create an IronMQ

- Navigate to the MQ section of the Iron.io dashboard, and create a new _unicast_ queue. For the subscriber URL, paste the Webhook URL of the IronWorker you created in the previous step.
- Copy the Webhook URL of your IronMQ, and use it to [subscribe](http://dev.commercetools.com/http-api-projects-subscriptions.html) to `OrderCreated` [message](http://dev.commercetools.com/http-api-projects-messages.html#ordercreated-message).

### Test

- Login with a valid customer account on your Sunrise app instance, and make sure to allow push notifications. Customer's push token will be stored in a [Customer](http://dev.commercetools.com/http-api-projects-customers.html#customer)'s [custom field](http://dev.commercetools.com/http-api-projects-custom-fields.html#customfields).
- Pick some product and make a reservation.
- The [Commercetools](https://commercetools.com) platform sends an `OrderCreated` message to the IronMQ you subscribed. The queue triggers the IronWorker, which retrieves customer's token from the Commercetools API, and sends the notification payload to the Apple's production notification server.