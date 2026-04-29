package com.salvatore.gymapp.job;

import com.salvatore.gymapp.dto.notification.PushNotificationPayload;
import com.salvatore.gymapp.entity.Subscription;
import com.salvatore.gymapp.entity.notification.PushSubscription;
import com.salvatore.gymapp.repository.PushSubscriptionRepository;
import com.salvatore.gymapp.repository.SubscriptionRepository;
import com.salvatore.gymapp.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationNotificationJob {

    private final SubscriptionRepository subscriptionRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;

   // @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Rome")
   @Scheduled(fixedDelayString = "${app.notifications.delay:86400000}")
    public void sendExpirationNotifications() {
        LocalDate today = LocalDate.now();

        List<LocalDate> targetDates = List.of(
                today,
                today.plusDays(2),
                today.plusDays(5)
        );

        List<Subscription> subscriptions =
                subscriptionRepository.findByActiveTrueAndEndDateIn(targetDates);

        log.info("Batch notifiche scadenza abbonamenti avviato. Subscription trovate: {}", subscriptions.size());

        for (Subscription subscription : subscriptions) {
            if (subscription.getUser() == null || subscription.getUser().getId() == null) {
                continue;
            }

            Long userId = subscription.getUser().getId();
            LocalDate endDate = subscription.getEndDate();

            List<PushSubscription> pushSubscriptions =
                    pushSubscriptionRepository.findByUserIdAndActiveTrue(userId);

            if (pushSubscriptions.isEmpty()) {
                log.info("Nessuna push subscription attiva per userId {}", userId);
                continue;
            }

            PushNotificationPayload payload = buildPayload(endDate, today);

            for (PushSubscription pushSubscription : pushSubscriptions) {
                webPushService.sendNotification(pushSubscription, payload);
            }
        }

        log.info("Batch notifiche scadenza abbonamenti completato");
    }

    private PushNotificationPayload buildPayload(LocalDate endDate, LocalDate today) {
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);

        String title;
        String body;

        if (daysLeft == 0) {
            title = "Abbonamento in scadenza oggi";
            body = "Il tuo abbonamento scade oggi. Rinnova per continuare ad allenarti senza interruzioni 💪";
        } else if (daysLeft == 2) {
            title = "Abbonamento in scadenza";
            body = "Il tuo abbonamento scade tra 2 giorni. Passa in palestra per rinnovarlo 🔔";
        } else {
            title = "Promemoria abbonamento";
            body = "Il tuo abbonamento scade tra 5 giorni. Meglio non arrivare all’ultimo 😉";
        }

        return new PushNotificationPayload(
                new PushNotificationPayload.NotificationData(
                        title,
                        body,
                        "/icons/icon-192x192.png",
                        "/icons/icon-192x192.png",
                        new PushNotificationPayload.Data("/app/workout"),
                        false,
                        new int[]{200, 100, 200}
                )
        );
    }
}