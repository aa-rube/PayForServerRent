package app.bot.controller;

import app.bot.config.BotConfig;
import app.bot.service.KeyBoards;
import app.command_executor.RemoteCommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

@Controller
public class Chat extends TelegramLongPollingBot {
    @Autowired
    private BotConfig botConfig;
    @Autowired
    private KeyBoards keyboard;
    private final HashMap<Long, Integer> chatIdMsgId = new HashMap<>();
    private final HashMap<Long, LocalDateTime> chattingWithAdmin = new HashMap<>();


    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().isReply()) {
            replayHandle(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            callBAckDataHandle(update);
            return;
        }

        if (update.hasMessage()) {
            textMessageHandle(update);
        }
    }

    private void replayHandle(Update update) {
        Long replyToMessageForwardFromChatId = update.getMessage().getReplyToMessage().getForwardFrom().getId();
        sendMsg(replyToMessageForwardFromChatId, update.getMessage().getText());
    }

    private void callBAckDataHandle(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        int msgId = update.getCallbackQuery().getMessage().getMessageId();
        String data = update.getCallbackQuery().getData();
        chatIdMsgId.put(chatId, msgId);

        if (data.equals("0")) {
            chattingWithAdmin.put(chatId, LocalDateTime.now().plusMinutes(15));
            editMessageKeyboard(chatId, msgId, "В течении 15минут с Вашего последнего сообщения, " +
                    "вы можете переписываться с админом", null);
        }
    }

    private void textMessageHandle(Update update) {
        Long chatId = update.getMessage().getChatId();
        int msgId = update.getMessage().getMessageId();
        String text = update.getMessage().getText();
        chatIdMsgId.put(chatId, msgId);

        if (chattingWithAdmin.containsKey(chatId)) {
            chattingWithAdmin.put(chatId, LocalDateTime.now().plusMinutes(15));
            forwardMessage(update.getMessage());
        }


        if (text.equals("/start")) {
            sendMsg(chatId, "Hello!", keyboard.sendMessageToAdmin());
        }

        if (text.equals("try")) {
            try {
                sendMsg(chatId, RemoteCommandExecutor.executeLocalCommand("ls"));
            } catch (IOException e) {
                sendMsg(chatId, "Что-то пошло не так");
            }
        }
    }

    private synchronized void forwardMessage(Message messageContent) {
        ForwardMessage forwardMessage = new ForwardMessage();

        forwardMessage.setChatId(botConfig.getAdminChatId());
        forwardMessage.setFromChatId(messageContent.getChatId().toString());
        forwardMessage.setMessageId(messageContent.getMessageId());

        try {
            execute(forwardMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRateString = "60000")
    private void stopChattingWithAdmin() {
        System.out.println("stopChattingWithAdmin");
        for (Long chatId : chattingWithAdmin.keySet()) {
            LocalDateTime now = LocalDateTime.now();
            if(chattingWithAdmin.get(chatId).isBefore(now)) {
                chattingWithAdmin.remove(chatId);
                sendMsg(chatId, "Чат с админом закрыт");
            }
        }
    }

    public void sendAndDelete(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            Message sentMessage = execute(message);
            deleteMessage(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(Long chatId, File file) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(file));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized void responseToUser(Long senderId, int messageId, String text) {
        SendMessage responseMessage = new SendMessage();
        responseMessage.setChatId(senderId);
        responseMessage.setText(text);
        responseMessage.setReplyToMessageId(messageId);
        responseMessage.setReplyMarkup(null);

        try {
            execute(responseMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMessageKeyboard(Long chatId, int messageId, String content, InlineKeyboardMarkup keyboard) {
        EditMessageText newMessage = new EditMessageText();
        newMessage.setChatId(chatId);
        newMessage.setMessageId(messageId);
        newMessage.setText(content);
        newMessage.setReplyMarkup(keyboard);
        try {
            execute(newMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editMessageText(long chatId, String text) {
        EditMessageText newMessage = new EditMessageText();
        newMessage.setChatId(chatId);
        newMessage.setMessageId(chatIdMsgId.get(chatId));
        newMessage.setText(text);
        newMessage.setReplyMarkup(null);
        try {
            execute(newMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        executeMsg(message);
    }

    private void sendMsg(Long chatId, String message, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        sendMessage.setReplyMarkup(keyboardMarkup);
        executeMsg(sendMessage);
    }

    private void sendMsg(Long chatId, String message, InlineKeyboardMarkup inlineKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(inlineKeyboard);
        executeMsg(sendMessage);
    }

    public void deleteMessage(Long chatId, int msgId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(msgId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void executeMsg(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}