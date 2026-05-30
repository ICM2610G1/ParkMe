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

    // Manejo para cuando se envía una imagen sin texto
    if (messageData.imageUrl && (!messageText || messageText === "")) {
        messageText = "📷 Imagen";
    }

    try {
        // 1. Obtener la sala de chat para saber quiénes participan
        const chatRoomRef = admin.firestore().collection("chats").doc(chatId);
        const chatRoomDoc = await chatRoomRef.get();
        
        if (!chatRoomDoc.exists) {
            logger.error(`La sala de chat ${chatId} no existe`);
            return;
        }

        const chatRoomData = chatRoomDoc.data();
        const userId = chatRoomData?.userId;
        const operatorId = chatRoomData?.operatorId;

        // 2. Determinar quién es el receptor (el que NO es el senderId)
        let receiverId = "";
        if (senderId === userId) {
            receiverId = operatorId; // El usuario envió, el operador recibe
        } else if (senderId === operatorId) {
            receiverId = userId;     // El operador envió, el usuario recibe
        } else {
            logger.error("El senderId no coincide ni con el usuario ni con el operador de esta sala.");
            return;
        }

        // 3. Buscar el token FCM del receptor en tu colección de usuarios
        const receiverDoc = await admin.firestore().collection("users").doc(receiverId).get();
        const receiverData = receiverDoc.data();

        if (!receiverData || !receiverData.fcmToken) {
            logger.info(`El receptor ${receiverId} no tiene un fcmToken guardado. No se puede enviar la notificación.`);
            return;
        }

        // 4. (Opcional) Obtener el nombre del remitente para que aparezca en el título
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        let senderTitle = senderDoc.data()?.name || "Nuevo mensaje";
        
        // Si el remitente es un operador, podrías preferir usar el nombre del parqueadero
        if (senderId === operatorId && chatRoomData?.parkingName) {
            senderTitle = chatRoomData.parkingName;
        }

        // 5. Construir y enviar la notificación
        const payload = {
            token: receiverData.fcmToken,
            notification: {
                title: senderTitle,
                body: messageText,
            },
            data: {
                chatId: chatId, // Para que la app sepa qué chat abrir al tocar la notificación
                click_action: "OPEN_CHAT_ACTIVITY" 
            }
        };

        await admin.messaging().send(payload);
        logger.info(`Notificación enviada exitosamente a ${receiverId}`);

    } catch (error) {
        logger.error("Error al procesar la notificación de chat:", error);
    }
});