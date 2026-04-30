package com.salvatore.gymapp.service;

import com.salvatore.gymapp.config.WebPushProperties;
import com.salvatore.gymapp.dto.notification.PushNotificationPayload;
import com.salvatore.gymapp.entity.notification.PushSubscription;
import com.salvatore.gymapp.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final WebPushProperties webPushProperties;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    public boolean sendNotification(PushSubscription subscription, PushNotificationPayload payload) {
        {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);

                Notification notification = new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        jsonPayload.getBytes(StandardCharsets.UTF_8)
                );

                PushService pushService = new PushService();
                pushService.setPublicKey(webPushProperties.getPublicKey());
                pushService.setPrivateKey(webPushProperties.getPrivateKey());
                pushService.setSubject(webPushProperties.getSubject());

                HttpResponse response = pushService.send(notification);

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 403 || statusCode == 404 || statusCode == 410) {
                    subscription.setActive(false);
                    pushSubscriptionRepository.save(subscription);

                    log.warn("Subscription push disattivata. Status: {}, ID: {}, Platform: {}",
                            statusCode,
                            subscription.getId(),
                            subscription.getPlatform()
                    );

                    return false;
                }

                if (statusCode >= 200 && statusCode < 300) {
                    log.info("Notifica push inviata correttamente. Subscription ID: {}", subscription.getId());
                    return true;
                }

                log.warn("Errore invio push. Status: {}, Subscription ID: {}", statusCode, subscription.getId());
                return false;

            } catch (Exception e) {
                log.error("Errore durante invio notifica push. Subscription ID: {}", subscription.getId(), e);
                return false;
            }
        }
    }
}