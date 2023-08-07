package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.pengrad.telegrambot.request.SendMessage;
import pro.sky.telegrambot.entities.NotificationTask;
import pro.sky.telegrambot.repositoties.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;
    @Autowired
    private TaskExecutionProperties taskExecutionProperties;
    @Autowired
    private NotificationTaskRepository repository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            if (update.message().text().equals("/start")) {
                sendMassageInChat(update.message().chat().id(), "Hello, " + update.message().chat().firstName() + "!");
                return;
            }

            addMessageTask(update);
        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    /**
     * Check every minute and send a message.
     * -----
     * Ежеминутная проверка и отправка напоминания.
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void checkTasksInDB() {
        LocalDateTime dateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> tasks = repository.findByDataTimeMessage(dateTime);

        if (!tasks.isEmpty()) {
            for (var task : tasks) {
                sendMassageInChat(task.getChatId(), "Напоминание: " + task.getMessage());
                repository.delete(task);
            }
        }

        // Проверка на опоздавшие задачи.
        List<NotificationTask> tasksLate = repository.findByDataTimeMessageBefore(dateTime);
        if (!tasksLate.isEmpty()) {
            for (var task : tasksLate) {
                sendMassageInChat(task.getChatId(), "Опозданием!\nДата и время: " + task.getDataTimeMessage() +
                        "\nНапоминание: " + task.getMessage());
                repository.delete(task);
            }
        }
    }

    /*Additional methods*/

    /**
     * Method for sending a message to a chat.
     * -----
     * Метод для отправки сообщения в чат.
    */
    private void sendMassageInChat(long chatId, String answer) {
        SendMessage message = new SendMessage(chatId, answer);
        SendResponse response = telegramBot.execute(message);
    }

    /**
     * Checking, creates and adds a task to the DB.
     * -----
     * Проверка, создание и добавление напонимания в БД.
    */
    private void addMessageTask(Update update) {
        String chatMessage = update.message().text();
        Long chatId = update.message().chat().id();
        String data;
        String message;

        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");

        Matcher matcher = pattern.matcher(chatMessage);

        if (matcher.matches()) {
            data = matcher.group(1);
            message = matcher.group(3);
        } else {
            sendMassageInChat(chatId, "Не корректно введена");
            return;
        }

        NotificationTask task = new NotificationTask();
        task.setChatId(chatId);
        task.setDataTimeMessage(LocalDateTime.parse(data, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        task.setMessage(message);

        repository.save(task);

        sendMassageInChat(chatId, "Напоминание добавленно!");
    }

    /*
     */
}
