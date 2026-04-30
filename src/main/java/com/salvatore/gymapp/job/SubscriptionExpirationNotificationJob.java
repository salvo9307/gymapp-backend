package com.salvatore.gymapp.job;

import com.salvatore.gymapp.dto.notification.PushNotificationPayload;
import com.salvatore.gymapp.entity.Subscription;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.entity.notification.PushNotificationLog;
import com.salvatore.gymapp.entity.notification.PushSubscription;
import com.salvatore.gymapp.repository.PushNotificationLogRepository;
import com.salvatore.gymapp.repository.PushSubscriptionRepository;
import com.salvatore.gymapp.repository.SubscriptionRepository;
import com.salvatore.gymapp.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationNotificationJob {

    private final SubscriptionRepository subscriptionRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushNotificationLogRepository pushNotificationLogRepository;
    private final WebPushService webPushService;

    @Scheduled(cron = "${app.notifications.cron:0 0 9 1 * *}", zone = "Europe/Rome")
    @Transactional
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
            User user = subscription.getUser();

            if (user == null || user.getId() == null) {
                continue;
            }

            if (!user.isActive()) {
                log.info("Notifica saltata: utente disattivato. userId={}", user.getId());
                continue;
            }

            Long userId = user.getId();
            LocalDate endDate = subscription.getEndDate();

            List<PushSubscription> pushSubscriptions =
                    pushSubscriptionRepository.findByUserIdAndActiveTrue(userId);

            if (pushSubscriptions.isEmpty()) {
                log.info("Nessuna push subscription attiva per userId {}", userId);
                continue;
            }

            String notificationType = resolveNotificationType(endDate, today);
            PushNotificationPayload payload = buildPayload(endDate, today);

            for (PushSubscription pushSubscription : pushSubscriptions) {
                boolean alreadySent =
                        pushNotificationLogRepository.existsByUser_IdAndSubscriptionIdAndNotificationTypeAndTargetDate(
                                userId,
                                pushSubscription.getId(),
                                notificationType,
                                endDate
                        );

                if (alreadySent) {
                    log.info(
                            "Notifica già inviata. userId={}, subscriptionId={}, type={}, targetDate={}",
                            userId,
                            pushSubscription.getId(),
                            notificationType,
                            endDate
                    );
                    continue;
                }

                boolean sent = webPushService.sendNotification(pushSubscription, payload);

                if (!sent) {
                    continue;
                }

                PushNotificationLog logEntity = new PushNotificationLog();
                logEntity.setUser(user);
                logEntity.setSubscriptionId(pushSubscription.getId());
                logEntity.setNotificationType(notificationType);
                logEntity.setTargetDate(endDate);

                pushNotificationLogRepository.save(logEntity);
            }
        }

        log.info("Batch notifiche scadenza abbonamenti completato");
    }

    private String resolveNotificationType(LocalDate endDate, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, endDate);

        if (daysLeft == 0) {
            return "EXPIRING_TODAY";
        }

        if (daysLeft == 2) {
            return "EXPIRING_2_DAYS";
        }

        if (daysLeft == 5) {
            return "EXPIRING_5_DAYS";
        }

        return "EXPIRING_UNKNOWN";
    }

    private PushNotificationPayload buildPayload(LocalDate endDate, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, endDate);

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