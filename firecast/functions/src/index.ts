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
            logger.error(`La sala de chat ${chatId} no existe`);
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
            logger.error("El senderId no coincide ni con el usuario ni con el operador de esta sala.");
            return;
        }

        const receiverDoc = await admin.firestore().collection("users").doc(receiverId).get();
        const receiverData = receiverDoc.data();

        if (!receiverData || !receiverData.fcmToken) {
            logger.info(`El receptor ${receiverId} no tiene un fcmToken guardado. No se puede enviar la notificación.`);
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
        logger.info(`Notificación enviada exitosamente a ${receiverId}`);

    } catch (error) {
        logger.error("Error al procesar la notificación de chat:", error);
    }
});