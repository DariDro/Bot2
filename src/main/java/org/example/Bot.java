package org.example;






import org.example.commands.AppBotCommand;
import org.example.commands.BotCommonCommands;
import org.example.functions.FilterOperations;
import org.example.functions.ImagesOperation;
import org.example.utils.ImageUtils;
import org.example.utils.PhotoMessageUtils;
import org.example.utils.RgbMaster;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Bot extends TelegramLongPollingBot {

    HashMap<String, Message> messages = new HashMap<>();

    @Override public void onUpdateReceived(Update update) { Message message = update.getMessage();
        try { SendMessage responseTextMessage = runCommonCommand(message); if (responseTextMessage != null) { execute(responseTextMessage);
            return; } responseTextMessage = runPhotoMessage(message); if (responseTextMessage != null) { execute(responseTextMessage);
                return; } Object responseMediaMessage = runPhotoFilter(message); if (responseMediaMessage != null) {
                    if (responseMediaMessage instanceof SendMediaGroup) { execute((SendMediaGroup) responseMediaMessage); }
                    else if (responseMediaMessage instanceof SendMessage) { execute((SendMessage) responseMediaMessage);
    } return;
                } } catch (InvocationTargetException | IllegalAccessException | TelegramApiException e) { e.printStackTrace(); } }





    private List<File> getFilesByMessage(Message message) { List<PhotoSize> photoSizes = message.getPhoto();
        if (photoSizes == null) return new ArrayList<>(); ArrayList<File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) { final String fileId = photoSize.getFileId();
        try {
            boolean add;
            add = files.add(sendApiMethod(new GetFile(fileId)));
        } catch (TelegramApiException e) { e.printStackTrace();
        } } return files; }







        private ReplyKeyboardMarkup getKeyboard() {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup() ;
            ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>();
     allKeyboardRows.addAll (getKeyboardRows(BotCommonCommands.class)) ;
      allKeyboardRows.addAll (getKeyboardRows(FilterOperations.class)) ;

            replyKeyboardMarkup.setKeyboard(allKeyboardRows) ;
            replyKeyboardMarkup.setOneTimeKeyboard(true) ;
            return replyKeyboardMarkup; }














    private SendMessage runPhotoMessage(Message message) {
        List<File> files = getFilesByMessage(message);
        if (files.isEmpty()) { return null; } String chatId = message.getChatId().toString(); messages.put(chatId, message);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>(getKeyboardRows(FilterOperations.class)); replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(replyKeyboardMarkup); sendMessage.setChatId(chatId); sendMessage.setText("Выберите фильтр");
        return sendMessage;
    }



















    private static ArrayList<KeyboardRow>  getKeyboardRows(Class someClass)

    { Method[] classMethods = someClass.getDeclaredMethods(); ArrayList<AppBotCommand> commands = new ArrayList<>();
        for (Method method : classMethods) { if (method.isAnnotationPresent(AppBotCommand.class)) { commands.add(method.getAnnotation(AppBotCommand.class));
        } } ArrayList<KeyboardRow> keyboardRows = new ArrayList<>()
            ; int columCount = 3;
            int rowCount = commands.size() / columCount + ((commands.size() % columCount == 0) ? 0 : 1);
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) { KeyboardRow row = new KeyboardRow();
                for (int columIndex = 0; columIndex < columCount; columIndex++) { int index = rowIndex * columCount + columIndex;
                    if (index >= commands.size()) continue; AppBotCommand command = commands.get(rowIndex * columCount + columIndex);
        KeyboardButton keyboardButton = new KeyboardButton(command.name());
        row.add(keyboardButton);
    } keyboardRows.add(row);
    } return keyboardRows;
    }

    private SendMediaGroup preparePhotoMessage(List<String> localPaths, ImagesOperation operation, String chatId)
            throws Exception { SendMediaGroup mediaGroup = new SendMediaGroup();
        ArrayList<InputMedia> medias = new ArrayList<>();
        for (String path : localPaths) { InputMedia inputMedia = new InputMediaPhoto(); processingImage(path, operation);
            inputMedia.setMedia(new java.io.File(path), "path");
        medias.add(inputMedia); } mediaGroup.setMedias(medias); mediaGroup.setChatId(chatId);
        return mediaGroup;
    }

    private String runCommand(String text) throws InvocationTargetException, IllegalAccessException
    { BotCommonCommands commands = new BotCommonCommands(); Method[] classMethods = commands.getClass().getDeclaredMethods();
        for (Method method : classMethods) { if (method.isAnnotationPresent(AppBotCommand.class))
        { AppBotCommand command = method.getAnnotation(AppBotCommand.class);
        if (command.name().equals(text)) { method.setAccessible(true);
            return (String) method.invoke(commands);
        } } } return null;
    }








    private Object runPhotoFilter(Message newMessage)
    { final String text = newMessage.getText();
        ImagesOperation operation = ImageUtils.getOperation(text);
        if (operation == null) return null; String chatId = newMessage.getChatId().toString();
        Message photoMessage = messages.get(chatId); if (photoMessage != null) {
        List<org.telegram.telegrambots.meta.api.objects.File> files = getFilesByMessage(photoMessage);
            try { List<String> paths = PhotoMessageUtils.savePhotos(files, getBotToken());
                return preparePhotoMessage(paths, operation, chatId); } catch (Exception e) { e.printStackTrace();
            } }
    else { SendMessage sendMessage = new SendMessage(); sendMessage.setChatId(chatId);
        sendMessage.setText("Мы конечно можем долго болтать, но у меня нет на это времени. Отправьте фото и я наложу тот фильтр, который понадобится");
        return sendMessage; } return null; }

























    private SendMessage runCommonCommand(Message message) throws InvocationTargetException, IllegalAccessException {
        String text = message.getText();
        BotCommonCommands commands = new BotCommonCommands();
        Method[] classMethods = commands.getClass().getDeclaredMethods();
        for (Method method : classMethods) {if (method.isAnnotationPresent(AppBotCommand.class)) {
        AppBotCommand command = method.getAnnotation(AppBotCommand.class);
        if (command.name().equals(text)) { method.setAccessible(true);
            String responseText = (String) method.invoke(commands);
            if (responseText != null) { SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(responseText);
        return sendMessage;
                }
              }
            }
         }
        return null;
    }

















//            final String localFileName = "receive_image.jpeg"; Message message = update.getMessage();
//            PhotoSize photoSize = message.getPhoto().get(0); final String fileIdd = photoSize.getFileId();
//            try {
//                // Нам приходится указать длинный путь, так как мы используем File из разных библиотек
//                 final org.telegram.telegrambots.meta.api.objects.File file = sendApiMethod(new GetFile(fileIdd));
//                 final String imageURL = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();
//
//                PhotoMessageUtils.saveImage(imageURL, localFileName);
//            } catch (TelegramApiException | IOException e) { e.printStackTrace();
//            } try
//            { processingImage(localFileName);
//
//            }
//
//            catch (Exception e) { throw new RuntimeException(e); } SendPhoto sendPhoto = new SendPhoto();
//                 sendPhoto.setChatId(message.getChatId().toString()); InputFile newFile = new InputFile(); newFile.setMedia(new File(localFileName));
//                 sendPhoto.setPhoto(newFile); sendPhoto.setCaption("Загруженное изображение"); try { execute(sendPhoto); } catch (TelegramApiException e)
//                 { e.printStackTrace(); } }
//
//



























//            Message message = update.getMessage();
//
//
//            PhotoSize photoSize = message.getPhoto().get(3);
//            final String fileIdd = photoSize.getFileId();
//            try {
//           // Ham npuходется ykasaTb длинный nyть, Tak Kak мы используем  File ua pa3hbix 6u6nuoTeK
//                final org.telegram.telegrambots.meta.api.objects.File file = sendApiMethod(new GetFile(fileIdd));
//                final String imageURL = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();
//                saveImage(imageURL, "images/receive_image.jpeg"); //,,,,,,,,,,,,,,,,,,,.
//            } catch (TelegramApiException | IOException e) {
//                throw new RuntimeException(e) ;
//
//            }
//
//
//
//            SendPhoto sendPhoto = new SendPhoto();
//            sendPhoto.setChatId(message.getChatId());
//
//            InputFile inputFile = new InputFile();
//            inputFile.setMedia(new File(  "images/receive_image.jpeg")); //путь к жесткому диску
//            sendPhoto.setPhoto(inputFile) ;
//            sendPhoto.setCaption("изображение");
//
//
//
//
//            try {
//
//                execute(sendPhoto);
//            } catch (TelegramApiException e) {
//              //throw new RuntimeException(e);
//                e.printStackTrace();
//            }
//












    public static void processingImage(String fileName, ImagesOperation operation) throws Exception
    { final BufferedImage image = ImageUtils.getImage(fileName);
        final RgbMaster rgbMaster = new RgbMaster(image); rgbMaster.changeImage(operation);
        ImageUtils.saveImage(rgbMaster.getImage(), fileName); }



        @Override
        public String getBotUsername () {
            return "pupok_ip_bot";


        }
        @Override
        public String getBotToken () {
            return "6363928597:AAGHgZN-o_IUjsvNeEL6WHxzBAu6_52fZes" ;

        }












        }



