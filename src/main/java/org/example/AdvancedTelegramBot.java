package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.*;

public class AdvancedTelegramBot extends TelegramLongPollingBot {

    private final DatabaseService db = new DatabaseService();
    private final AdminService adminService = new AdminService(db);
    public final Map<Long, Boolean> awaitingAdminPassword = new HashMap<>();

    private final List<String> requiredChannels = List.of(
            "@argos_testlarim",
            "@gibridtest",
            "@gibridtesthamshira"
    );

    @Override
    public String getBotUsername() {
        return "@ARGOSGIBRIDTESTBOT";
    }

    @Override
    public String getBotToken() {
        return "8587886208:AAH2g1PJdEtis0vYbel-RlbCoaVQ8T-tUeE";
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (adminService.handleAdminCommands(update, this)) return;

            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                String text = update.getMessage().getText();
                String username = update.getMessage().getFrom().getUserName();
                String firstName = update.getMessage().getFrom().getFirstName();

                db.saveUser(chatId, username, firstName);

                if (text.equals("/start")) {
                    sendStartWithInlineChannels(chatId);
                } else if (text.equals("\uD83E\uDDE0 Тест ишлаш")) {
                    // 1️⃣ Kanalga obuna tekshiruvi
                    List<String> unsubscribed = getUnsubscribedChannels(chatId);

                    if (unsubscribed.isEmpty()) {
                        // 2️⃣ Obuna bo‘lgan – testni boshlash tugmasi chiqadi
                        sendWebAppWithAutoHide(chatId);
                    } else {
                        // 3️⃣ Obuna bo‘lmagan – kanal ro‘yxatini ko‘rsatadi
                        sendUnsubscribedMessage(chatId, unsubscribed);
                    }
                }

            }

            if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

                if (data.equals("check_subs")) {
                    List<String> unsubscribed = getUnsubscribedChannels(chatId);
                    if (unsubscribed.isEmpty()) {
                        sendAccessGranted(chatId);
                    } else {
                        updateUnsubscribedChannels(chatId, messageId, unsubscribed);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendWebAppWithAutoHide(Long chatId) throws Exception {
        // 1️⃣ WebApp tugmasi chiqadi
        SendMessage msg = new SendMessage(chatId.toString(),
                "\uD83C\uDF10 Тестни бошлаш учун қуйидаги тугмани босинг:");

        KeyboardButton webButton = new KeyboardButton("\uD83D\uDCCB Тестни очиш");
        webButton.setWebApp(new WebAppInfo("https://gibridargos.github.io/gibridargos/"));

        KeyboardRow row = new KeyboardRow();
        row.add(webButton);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        msg.setReplyMarkup(markup);
        execute(msg);

        // 2️⃣ 2 soniya kutamiz
        Thread.sleep(5000);

        // 3️⃣ Yana “Тест ишлаш” tugmasini qaytarish
        KeyboardButton testBtn = new KeyboardButton("\uD83E\uDDE0 Тест ишлаш");
        KeyboardRow newRow = new KeyboardRow();
        newRow.add(testBtn);

        ReplyKeyboardMarkup backMarkup = new ReplyKeyboardMarkup(List.of(newRow));
        backMarkup.setResizeKeyboard(true);

        SendMessage backMsg = new SendMessage(chatId.toString(),
                "↩️ Тугма вақтинча олиб ташланди. Яна тест ишлаш учун босинг:");
        backMsg.setReplyMarkup(backMarkup);
        execute(backMsg);
    }

    private List<String> getUnsubscribedChannels(Long chatId) {
        List<String> notJoined = new ArrayList<>();
        for (String channel : requiredChannels) {
            try {
                GetChatMember chatMember = new GetChatMember(channel, chatId);
                ChatMember member = execute(chatMember);
                String status = member.getStatus();

                if (status.equals("left") || status.equals("kicked")) {
                    notJoined.add(channel);
                }
            } catch (Exception e) {
                notJoined.add(channel);
            }
        }
        return notJoined;
    }


    private void sendUnsubscribedMessage(Long chatId, List<String> unsubscribed) {
        try {
            StringBuilder sb = new StringBuilder("\uD83D\uDE43 Сиз канал ёки гуруҳга обуна бўлмагансиз!\n\n");
            sb.append("\uD83C\uDFAF Қуйидаги тугмалар орқали обуна бўлинг ва яна уриниб кўринг:\n\n");

            Map<String, String> channelNamesMap = new HashMap<>();
            channelNamesMap.put("@argos_testlarim", "АРГОС ТЕСТЛАРИ КАНАЛИ");
            channelNamesMap.put("@gibridtest", "ГИБРИД ТЕСТ УАШ КАНАЛИ");
            channelNamesMap.put("@gibridtesthamshira", "ГИБРИД ТЕСТ ХАМШИРА КАНАЛИ");

            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            for (String ch : unsubscribed) {
                String displayName = channelNamesMap.getOrDefault(ch, ch);
                InlineKeyboardButton btn = new InlineKeyboardButton("📢 " + displayName);
                btn.setUrl("https://t.me/" + ch.substring(1));
                buttons.add(List.of(btn));
            }

            sb.append("\n🔄 Обуна бўлгач, қайта текширинг:");
            InlineKeyboardButton checkBtn = new InlineKeyboardButton("✅ Қайта текшириш");
            checkBtn.setCallbackData("check_subs");
            buttons.add(List.of(checkBtn));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

            SendMessage msg = new SendMessage(chatId.toString(), sb.toString());
            msg.setReplyMarkup(markup);
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private void sendStartWithInlineChannels(Long chatId) throws Exception {
        String text = " \uD83D\uDCAC Ассалому алайкум. Ботимизга хуш келибсиз.\n\n" +
                "✅ СИНОВ ТЕСТЛАРИ СИЗНИ КУТМОКДА!!!\n\n" +
                "\uD83D\uDD3B Тест ишлаш учун канал ва гурухимизга обуна булинг.";

        Map<String, String> channelNamesMap = new HashMap<>();
        channelNamesMap.put("@argos_testlarim", "АРГОС ТЕСТЛАРИ КАНАЛИ");
        channelNamesMap.put("@gibridtest", "ГИБРИД ТЕСТ УАШ КАНАЛИ");
        channelNamesMap.put("@gibridtesthamshira", "ГИБРИД ТЕСТ ХАМШИРА КАНАЛИ");

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (String ch : requiredChannels) {
            String displayName = channelNamesMap.getOrDefault(ch, ch);
            InlineKeyboardButton btn = new InlineKeyboardButton("📢 " + displayName);
            btn.setUrl("https://t.me/" + ch.substring(1));
            buttons.add(List.of(btn));
        }

        InlineKeyboardButton checkBtn = new InlineKeyboardButton("✅ Обунани текшириш");
        checkBtn.setCallbackData("check_subs");
        buttons.add(List.of(checkBtn));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setReplyMarkup(markup);
        execute(msg);
    }

    private void updateUnsubscribedChannels(Long chatId, Integer messageId, List<String> unsubscribed) {
        try {
            StringBuilder sb = new StringBuilder("\uD83D\uDE43 Сиз канал ёки гуруҳга обуна бўлмагансиз!\n\n");
            sb.append("\uD83C\uDFAF Қуйидаги тугмалар орқали обуна бўлинг ва яна уриниб кўринг:\n\n");

            Map<String, String> channelNamesMap = new HashMap<>();
            channelNamesMap.put("@argos_testlarim", "АРГОС ТЕСТЛАРИ КАНАЛИ");
            channelNamesMap.put("@gibridtest", "ГИБРИД ТЕСТ УАШ КАНАЛИ");
            channelNamesMap.put("@gibridtesthamshira", "ГИБРИД ТЕСТ ХАМШИРА КАНАЛИ");

            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            for (String ch : unsubscribed) {
                String displayName = channelNamesMap.getOrDefault(ch, ch);
                sb.append("👉 ").append(displayName).append("\n");

                InlineKeyboardButton btn = new InlineKeyboardButton("📢 " + displayName);
                btn.setUrl("https://t.me/" + ch.substring(1));
                buttons.add(List.of(btn));
            }

            sb.append("\n🔄 Обуна бўлгач, қайта текширинг:");
            InlineKeyboardButton checkBtn = new InlineKeyboardButton("✅ Қайта текшириш");
            checkBtn.setCallbackData("check_subs");
            buttons.add(List.of(checkBtn));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);

            EditMessageText editMsg = new EditMessageText();
            editMsg.setChatId(chatId.toString());
            editMsg.setMessageId(messageId);
            editMsg.setText(sb.toString());
            editMsg.setReplyMarkup(markup);

            execute(editMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAccessGranted(Long chatId) throws Exception {
        SendMessage msg = new SendMessage(chatId.toString(),
                "\uD83C\uDF89 Ажойиб! Сиз барча каналларга обуна бўлгансиз.\n" +
                        "Энди тестни бошлашингиз мумкин \uD83D\uDC47");

        KeyboardButton testBtn = new KeyboardButton("\uD83E\uDDE0 Тест ишлаш");
        KeyboardRow row = new KeyboardRow();
        row.add(testBtn);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        msg.setReplyMarkup(markup);

        execute(msg);
    }
/*
    private void sendWebApp(Long chatId) throws Exception {
        SendMessage msg = new SendMessage(chatId.toString(),
                "\uD83C\uDF10 Тестни бошлаш учун қуйидаги тугмани босинг:");

        // 🟢 WebApp ochuvchi Replay tugma yaratamiz
        KeyboardButton webButton = new KeyboardButton("\uD83D\uDE80 Тестни бошлаш");
        webButton.setWebApp(new WebAppInfo("https://gibridargos.github.io/gibridargos/"));

        // 🧩 Tugmani qatorga joylaymiz
        KeyboardRow row = new KeyboardRow();
        row.add(webButton);

        // 🧱 Klaviatura tuzilmasi
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false); // foydalanuvchi bosgandan keyin yo‘qolmasin

        msg.setReplyMarkup(markup);

        execute(msg);
    }
*/

    public void executeSafely(SendMessage message) {
        try { execute(message); } catch (Exception e) { e.printStackTrace(); }
    }

    public void executeSafely(SendPhoto photo) {
        try { execute(photo); } catch (Exception e) { e.printStackTrace(); }
    }

    public void showAdminMenu(Long chatId) {
        KeyboardButton sendMsgBtn = new KeyboardButton("📢 Habar yuborish");
        KeyboardRow row = new KeyboardRow();
        row.add(sendMsgBtn);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);

        SendMessage msg = new SendMessage(chatId.toString(), "🔐 Admin menyusi:");
        msg.setReplyMarkup(markup);
        executeSafely(msg);
    }
}
