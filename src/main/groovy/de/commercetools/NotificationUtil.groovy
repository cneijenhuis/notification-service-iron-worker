package de.commercetools

import com.notnoop.apns.APNS
import com.notnoop.apns.ApnsService
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import io.iron.ironworker.client.helpers.WorkerHelper

import java.nio.charset.StandardCharsets

import static groovyx.net.http.ContentType.JSON

public static void main(String[] args) {
    WorkerHelper helper = WorkerHelper.fromArgs(args)
    InputStream jsonPayload = new FileInputStream(helper.payloadPath)

    JsonSlurper slurper = new JsonSlurper()
    Map map = slurper.parse(jsonPayload)

    sendReservationNotification(map)
}

class NotificationService {

    private String PROJECT_KEY = ''
    private String SCOPE = ''
    private String CLIENT_ID = ''
    private String CLIENT_SECRET = ''
    private String AUTH_URL = 'https://auth.sphere.io/'
    private String API_URL = 'https://api.sphere.io/'

    ApnsService apnsService

    NotificationService() {
        InputStream certificate = this.getClass().getResourceAsStream('/Sunrise.p12')
        apnsService = APNS.newService()
                .withCert(certificate, 'password')
                .withProductionDestination()
                .build()
    }

    void sendReservationNotification(Map order) {
        if (order?.custom?.fields?.isReservation) {
            String reservationPayload = "{\"aps\":{\"alert\":\"Hello, your item is ready for pickup.\",\"category\":\"reservation_confirmation\"},\"reservation-id\":\"${order.id}\"}"
            pushNotification(order.customerId, reservationPayload)
        }
    }

    private void pushNotification(String userId, String payload) {
        String userToken = retrieveTokenForUserId(userId)
        if (userToken) {
            apnsService.push(userToken, payload)
        }
    }

    private String retrieveTokenForUserId(String userId) {
        if (userId == null) {
            return null
        }
        String accessToken = getAccessToken()
        def commercetoolsClient = new RESTClient("${API_URL}${PROJECT_KEY}/customers/${userId}")
        def response = commercetoolsClient.get(requestContentType: JSON, headers: [Authorization: "Bearer ${accessToken}"])
        return response.data?.custom?.fields?.apnsToken
    }

    private String getAccessToken() {
        def authClient = new RESTClient("${AUTH_URL}oauth/token?grant_type=client_credentials&scope=${SCOPE}")
        def response = authClient.post(requestContentType: JSON, headers: [Authorization: "Basic ${Base64.encoder.encodeToString("${CLIENT_ID}:${CLIENT_SECRET}".getBytes(StandardCharsets.UTF_8))}"])
        return response.data?.access_token
    }
}
