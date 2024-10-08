package parser;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import commands.ByeCommand;
import commands.Command;
import commands.DeadlineCommand;
import commands.DeleteCommand;
import commands.EventCommand;
import commands.FindCommand;
import commands.ListCommand;
import commands.MarkCommand;
import commands.TodoCommand;
import commands.UnmarkCommand;
import exception.PrimoException;
import tasks.DeadlineTask;
import tasks.EventTask;
import tasks.TodoTask;

/**
 * The Parser class is responsible for interpreting user commands and converting
 * them into corresponding {@link Command} objects. It parses the input command string
 * and creates commands like {@link TodoCommand}, {@link DeadlineCommand}, {@link EventCommand},
 * {@link MarkCommand}, {@link UnmarkCommand}, and {@link DeleteCommand} based on the command type.
 */
public class Parser {

    /**
     * Enum to represent different types of commands.
     */
    enum CommandType {
        BYE("bye"),
        LIST("list"),
        MARK("mark"),
        UNMARK("unmark"),
        TODO("todo"),
        DEADLINE("deadline"),
        EVENT("event"),
        DELETE("delete"),
        FIND("find");

        private String command;

        CommandType(String command) {
            this.command = command;
        }

        /**
         * Gets the CommandType corresponding to the given command string.
         *
         * @param s The command string to be matched with an enum value.
         * @return The corresponding CommandType.
         * @throws PrimoException If the command string does not match any known command type.
         */
        public static CommandType getCommandType(String s) throws PrimoException {
            for (CommandType type : values()) {
                if (type.command.equals(s)) {
                    return type;
                }
            }
            throw new PrimoException("Invalid command!\n(Expected Commands: \ntodo, deadline, event, mark, unmark, "
                    + "delete, list, find, bye) \n\n(TIP: Try adding /n <note> at the back of command!)\n");
        }
    }

    /**
     * Parses the full command string and returns the appropriate {@link Command} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return The corresponding Command object based on the parsed command string.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    public static Command parse(String fullCommand) throws PrimoException {
        String[] wordsOfCommand = fullCommand.split(" ");
        CommandType type = CommandType.getCommandType(wordsOfCommand[0]);

        switch (type) {
        case BYE:
            assert wordsOfCommand[0].equals("bye");
            return new ByeCommand();
        case LIST:
            assert wordsOfCommand[0].equals("list");
            return new ListCommand();
        case MARK:
            return processMarkCommand(wordsOfCommand);
        case UNMARK:
            return processUnmarkCommand(wordsOfCommand);
        case TODO:
            return processTodoCommand(fullCommand);
        case DEADLINE:
            return processDeadlineCommand(fullCommand);
        case EVENT:
            return processEventCommand(fullCommand);
        case DELETE:
            return processDeleteCommand(wordsOfCommand);
        case FIND:
            return processFindCommand(wordsOfCommand);
        default:
            return null; // should not reach here if command type is valid
        }
    }

    /**
     * Processes the find command and returns the corresponding {@link FindCommand} object.
     *
     * @param wordsOfCommand The split command string array.
     * @return A FindCommand object containing the search string.
     * @throws PrimoException If the parameters are invalid or missing.
     */
    private static FindCommand processFindCommand(String[] wordsOfCommand) throws PrimoException {
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected find <string>");
        }
        assert wordsOfCommand[0].equals("find");
        if (wordsOfCommand.length <= 1) {
            throw new PrimoException("Poor formatting! Expecting find <string>");
        }
        return new FindCommand(wordsOfCommand[1]);
    }

    /**
     * Processes the delete command and returns the corresponding {@link DeleteCommand} object.
     *
     * @param wordsOfCommand The split command string array.
     * @return A DeleteCommand object containing the index of the task to delete.
     * @throws PrimoException If the parameters are invalid or not an integer.
     */
    private static DeleteCommand processDeleteCommand(String[] wordsOfCommand) throws PrimoException {
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected delete <integer>");
        }
        assert wordsOfCommand[0].equals("delete");
        try {
            Integer.valueOf(wordsOfCommand[1]);
        } catch (NumberFormatException e) {
            throw new PrimoException("delete <integer> expected");
        }
        int deleteIndex = Integer.valueOf(wordsOfCommand[1]) - 1;
        return new DeleteCommand(deleteIndex);
    }

    /**
     * Processes the event command and returns the corresponding {@link EventCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return An EventCommand object containing the event task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static EventCommand processEventCommand(String fullCommand) throws PrimoException {
        if (fullCommand.contains("/n")) {
            return eventCommandWithNote(fullCommand);
        } else {
            return eventCommandWithoutNote(fullCommand);
        }
    }

    /**
     * Processes the event command when a note is provided and returns the corresponding {@link EventCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return An EventCommand object containing the event task with a note.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static EventCommand eventCommandWithNote(String fullCommand) throws PrimoException {
        boolean containsFrom = fullCommand.contains("/from");
        boolean containsTo = fullCommand.contains("/to");
        if (!containsFrom || !containsTo) {     // guard against commands without /from and /to
            throw new PrimoException("Invalid parameters! Expected: event <string> /from <string> /to <string>");
        }

        int eventNameIndex = fullCommand.indexOf("event ");
        int eventFromIndex = fullCommand.indexOf("/from");
        int eventToIndex = fullCommand.indexOf("/to");
        int eventNoteIndex = fullCommand.indexOf("/n");

        String eventDescription = fullCommand.substring(eventNameIndex + 6, eventFromIndex).trim();
        String eventNote = fullCommand.substring(eventNoteIndex + 2).trim();
        String eventFromDateString = fullCommand.substring(eventFromIndex + 5, eventToIndex).trim();
        String eventToDateString = fullCommand.substring(eventToIndex + 3, eventNoteIndex).trim();

        if (eventDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected deadline <string> /by <string>");
        }
        if (eventFromDateString.isEmpty()) {
            throw new PrimoException("'From' parameter empty or wrong formatting! Expected event "
                    + "/from YYYY-MM-DD /to YYYY-MM-DD");
        }
        if (eventToDateString.isEmpty()) {
            throw new PrimoException("'To' parameter cannot be empty! Expected deadline YYYY-MM-DD /by YYYY-MM-DD");
        }

        EventTask newEventTask;
        try {
            newEventTask = new EventTask(eventDescription, eventFromDateString, eventToDateString, eventNote);
        } catch (DateTimeParseException e) {
            throw new PrimoException("Date formats are not in the form of YYYY-MM-DD");
        }
        return new EventCommand(newEventTask);
    }

    /**
     * Processes the event command when no note is provided and returns the corresponding {@link EventCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return An EventCommand object containing the event task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static EventCommand eventCommandWithoutNote(String fullCommand) throws PrimoException {
        boolean containsFrom = fullCommand.contains("/from");
        boolean containsTo = fullCommand.contains("/to");
        if (!containsFrom || !containsTo) {     // guard against commands without /from and /to
            throw new PrimoException("Invalid parameters! Expected: event <string> /from <string> /to <string>");
        }

        int eventNameIndex = fullCommand.indexOf("event ");
        int eventFromIndex = fullCommand.indexOf("/from");
        int eventToIndex = fullCommand.indexOf("/to");

        String eventDescription = fullCommand.substring(eventNameIndex + 6, eventFromIndex).trim();
        String eventFromDateString = fullCommand.substring(eventFromIndex + 5, eventToIndex).trim();
        String eventToDateString = fullCommand.substring(eventToIndex + 3).trim();

        if (eventDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected deadline <string> /by <string>");
        }
        if (eventFromDateString.isEmpty()) {
            throw new PrimoException("'From' parameter empty or wrong formatting! Expected event "
                    + "/from YYYY-MM-DD /to YYYY-MM-DD");
        }
        if (eventToDateString.isEmpty()) {
            throw new PrimoException("'To' parameter cannot be empty! Expected deadline YYYY-MM-DD /by YYYY-MM-DD");
        }

        EventTask newEventTask;
        try {
            newEventTask = new EventTask(eventDescription, eventFromDateString, eventToDateString);
        } catch (DateTimeParseException e) {
            throw new PrimoException("Date formats are not in the form of YYYY-MM-DD");
        }
        return new EventCommand(newEventTask);
    }

    /**
     * Processes the deadline command and returns the corresponding {@link DeadlineCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A DeadlineCommand object containing the deadline task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static DeadlineCommand processDeadlineCommand(String fullCommand) throws PrimoException {
        if (fullCommand.contains("/n")) {
            return deadlineCommandWithNote(fullCommand);
        } else {
            return deadlineCommandWithoutNote(fullCommand);
        }
    }

    /**
     * Processes the deadline command when a note is provided and returns the corresponding {@link DeadlineCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A DeadlineCommand object containing the deadline task with a note.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static DeadlineCommand deadlineCommandWithNote(String fullCommand) throws PrimoException {
        if (!fullCommand.contains("/by")) {     // guard against commands without /by
            throw new PrimoException("Invalid parameters! Expected: deadline <string> /by <string>");
        }

        int deadlineNameIndex = fullCommand.indexOf("deadline ");
        int deadlineByIndex = fullCommand.indexOf("/by");
        int deadlineNoteIndex = fullCommand.indexOf("/n");

        String deadlineDescription = fullCommand.substring(deadlineNameIndex + 9, deadlineByIndex).trim();
        String deadlineDateString = fullCommand.substring(deadlineByIndex + 3, deadlineNoteIndex).trim();
        String deadlineNoteString = fullCommand.substring(deadlineNoteIndex + 2).trim();

        if (deadlineDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected deadline <string> /by <string>");
        }
        if (deadlineDateString.isEmpty()) {
            throw new PrimoException("Deadline time empty! Expected deadline <string> "
                    + "/by YYYY-MM-DD");
        }

        DeadlineTask newDeadlineTask;
        try {
            newDeadlineTask = new DeadlineTask(deadlineDescription, deadlineDateString, deadlineNoteString);
        } catch (DateTimeParseException e) {
            throw new PrimoException("Deadline not in the form of YYYY-MM-DD or invalid DATE");
        }
        return new DeadlineCommand(newDeadlineTask);
    }

    /**
     * Processes the deadline command when no note is provided and returns the corresponding {@link DeadlineCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A DeadlineCommand object containing the deadline task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static DeadlineCommand deadlineCommandWithoutNote(String fullCommand) throws PrimoException {
        if (!fullCommand.contains("/by")) {     // guard against commands without /by
            throw new PrimoException("Invalid parameters! Expected: deadline <string> /by <string>");
        }

        int deadlineNameIndex = fullCommand.indexOf("deadline ");
        int deadlineByIndex = fullCommand.indexOf("/by");

        String deadlineDescription = fullCommand.substring(deadlineNameIndex + 9, deadlineByIndex).trim();
        String deadlineDateString = fullCommand.substring(deadlineByIndex + 3).trim();
        if (deadlineDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected deadline <string> /by <string>");
        }
        if (deadlineDateString.isEmpty()) {
            throw new PrimoException("Deadline time empty! Expected deadline <string> "
                    + "/by YYYY-MM-DD");
        }

        DeadlineTask newDeadlineTask;
        try {
            newDeadlineTask = new DeadlineTask(deadlineDescription, deadlineDateString);
        } catch (DateTimeParseException e) {
            throw new PrimoException("Deadline not in the form of YYYY-MM-DD or invalid DATE");
        }
        return new DeadlineCommand(newDeadlineTask);
    }

    /**
     * Processes the todo command and returns the corresponding {@link TodoCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A TodoCommand object containing the todo task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static TodoCommand processTodoCommand(String fullCommand) throws PrimoException {
        if (fullCommand.contains("/n")) {
            return todoCommandWithNote(fullCommand);
        } else {
            return todoCommandWithoutNote(fullCommand);
        }
    }

    /**
     * Processes the todo command when a note is provided and returns the corresponding {@link TodoCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A TodoCommand object containing the todo task with a note.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static TodoCommand todoCommandWithNote(String fullCommand) throws PrimoException {
        String[] wordsOfCommand = fullCommand.split(" ");
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected todo <string>");
        }
        assert wordsOfCommand[0].equals("todo");
        assert wordsOfCommand.length >= 2;
        int todoNameIndex = fullCommand.indexOf("todo ") + 5;
        int todoNoteIndex = fullCommand.indexOf("/n");
        String todoDescription = fullCommand.substring(todoNameIndex, todoNoteIndex).trim();
        String todoNote = fullCommand.substring(todoNoteIndex + 2).trim();
        if (todoDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected: todo <string>");
        }

        TodoTask newTodoTask = new TodoTask(todoDescription, todoNote);
        System.out.println(newTodoTask);
        return new TodoCommand(newTodoTask);
    }

    /**
     * Processes the todo command when no note is provided and returns the corresponding {@link TodoCommand} object.
     *
     * @param fullCommand The full command string input by the user.
     * @return A TodoCommand object containing the todo task.
     * @throws PrimoException If the command format is invalid or parameters are missing/incorrect.
     */
    private static TodoCommand todoCommandWithoutNote(String fullCommand) throws PrimoException {
        String[] wordsOfCommand = fullCommand.split(" ");
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected todo <string>");
        }
        assert wordsOfCommand[0].equals("todo");
        assert wordsOfCommand.length >= 2;
        int todoNameIndex = fullCommand.indexOf("todo ") + 5;
        String todoDescription = fullCommand.substring(todoNameIndex).trim();
        if (todoDescription.isEmpty()) {
            throw new PrimoException("Description cannot be empty! Expected: todo <string>");
        }

        TodoTask newTodoTask = new TodoTask(todoDescription);
        return new TodoCommand(newTodoTask);
    }

    /**
     * Processes the unmark command and returns the corresponding {@link UnmarkCommand} object.
     *
     * @param wordsOfCommand The split command string array.
     * @return An UnmarkCommand object containing the index of the task to unmark.
     * @throws PrimoException If the parameters are invalid or not an integer.
     */
    private static UnmarkCommand processUnmarkCommand(String[] wordsOfCommand) throws PrimoException {
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected unmark <integer>");
        }
        assert wordsOfCommand[0].equals("unmark");
        try {
            Integer.valueOf(wordsOfCommand[1]);
        } catch (NumberFormatException e) {
            throw new PrimoException("unmark <integer> expected");
        }
        int unmarkIndex = Integer.parseInt(wordsOfCommand[1]) - 1;
        return new UnmarkCommand(unmarkIndex);
    }

    /**
     * Processes the mark command and returns the corresponding {@link MarkCommand} object.
     *
     * @param wordsOfCommand The split command string array.
     * @return A MarkCommand object containing the index of the task to mark.
     * @throws PrimoException If the parameters are invalid or not an integer.
     */
    private static MarkCommand processMarkCommand(String[] wordsOfCommand) throws PrimoException {
        if (wordsOfCommand.length == 1) {
            throw new PrimoException("Invalid parameters! Expected mark <integer>");
        }
        assert wordsOfCommand[0].equals("mark");
        try {
            Integer.valueOf(wordsOfCommand[1]);
        } catch (NumberFormatException e) {
            throw new PrimoException("mark <integer> expected");
        }
        int markIndex = Integer.parseInt(wordsOfCommand[1]) - 1;
        return new MarkCommand(markIndex);
    }
}
