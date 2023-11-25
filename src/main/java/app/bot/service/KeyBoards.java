package app.bot.service;

import app.bot.model.Buttons;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyBoards {
    public InlineKeyboardMarkup sendMessageToAdmin() {
        InlineKeyboardMarkup inLineKeyBoard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton msg = new InlineKeyboardButton();

        msg.setText("Начать переписку");
        msg.setCallbackData(Buttons.SENT_TO_ADMIN.getNumber());

        firstRow.add(msg);
        keyboardMatrix.add(firstRow);
        inLineKeyBoard.setKeyboard(keyboardMatrix);
        return inLineKeyBoard;
    }
}
