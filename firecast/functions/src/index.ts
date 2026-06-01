import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

export const sendChatNotification = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const messageData = event.data?.data();
    if (!messageData) return;

    const chatId = event.params.chatId;
    const senderId = messageData.senderId;
    let messageText = messageData.text;


    if (messageData.imageUrl && (!messageText || messageText === "")) {
        messageText = "Imagen";
    }

    try {
        const chatRoomRef = admin.firestore().collection("chats").doc(chatId);
        const chatRoomDoc = await chatRoomRef.get();

        if (!chatRoomDoc.exists) {
            logger.error(`Chat room ${chatId} does not exist`);
            return;
        }

        const chatRoomData = chatRoomDoc.data();
        const userId = chatRoomData?.userId;
        const operatorId = chatRoomData?.operatorId;


        let receiverId = "";
        if (senderId === userId) {
            receiverId = operatorId;
        } else if (senderId === operatorId) {
            receiverId = userId;
        } else {
            logger.error("Sender ID does not match user or operator of this room.");
            return;
        }

        const receiverDoc = await admin.firestore().collection("users").doc(receiverId).get();
        const receiverData = receiverDoc.data();

        if (!receiverData || !receiverData.fcmToken) {
            logger.info(`Receiver ${receiverId} does not have an FCM token saved. Cannot send notification.`);
            return;
        }

        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        let senderTitle = senderDoc.data()?.name || "Nuevo mensaje";

        if (senderId === operatorId && chatRoomData?.parkingName) {
            senderTitle = chatRoomData.parkingName;
        }

        const payload = {
            token: receiverData.fcmToken,
            notification: {
                title: senderTitle,
                body: messageText,
            },
            data: {
                chatId: chatId,
                click_action: "OPEN_CHAT_ACTIVITY"
            }
        };

        await admin.messaging().send(payload);
        logger.info(`Chat notification sent successfully to ${receiverId}`);

    } catch (error) {
        logger.error("Error processing chat notification:", error);
    }
});

export const sendReservationNotification = onDocumentCreated("reservas/{resId}", async (event) => {
    const reservationData = event.data?.data();
    if (!reservationData) return;

    const operatorId = reservationData.operatorId;

    try {
        const receiverDoc = await admin.firestore().collection("users").doc(operatorId).get();
        const fcmToken = receiverDoc.data()?.fcmToken;

        if (!fcmToken) {
            logger.info(`Operator ${operatorId} does not have an FCM token saved. Cannot send reservation update.`);
            return;
        }
        const initialEtaSeconds = reservationData.etaSeconds || 900;
        const driverName = reservationData.userName || "Un conductor";

        const payload = {
            token: fcmToken,
            data: {
                type: "RESERVATION_UPDATE",
                reservationId: event.params.resId,
                userName: driverName,
                eta: initialEtaSeconds.toString(),
                status: "en_camino"
            }
        };

        await admin.messaging().send(payload);
        logger.info(`Uber-style reservation notification sent successfully to operator ${operatorId}`);

    } catch (error) {
        logger.error("Error processing reservation notification:", error);
    }
});