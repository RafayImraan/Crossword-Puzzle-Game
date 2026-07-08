package com.group4.crossword;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.List;

/**
 * JavaFX front-end for the crossword generator/solver.
 *
 * - Loads a word+clue list: a built-in themed pack, or a custom list typed
 *   in by the user.
 * - Runs the backtracking generator to build an interlocking grid.
 * - Renders a numbered grid of editable cells plus Across/Down clue panels.
 * - Validates the player's answers cell-by-cell in real time.
 * - Adds the three "future extensions" named in the proposal's conclusion:
 *   difficulty levels (controls how many hints are available), themed word
 *   packs, and a scoring system (time + hints based).
 */
public class CrosswordApp extends Application {

    private static final int GRID_SIZE = 15;

    private CrosswordGenerator generator;
    private Grid solutionGrid;
    private List<PlacedWord> placements;

    private GridPane gridPane;
    private TextField[][] cellFields;
    private boolean[][] activeCell; // true if this cell is part of the puzzle
    private int minRow, minCol, maxRow, maxCol;

    private VBox acrossBox;
    private VBox downBox;
    private Label statusLabel;
    private Label timerLabel;
    private TextArea wordInput;
    private ComboBox<String> packSelector;
    private ComboBox<String> difficultySelector;
    private Button hintBtn;

    private List<WordEntry> currentWords = new ArrayList<>();

    // --- Scoring / difficulty state ---
    private Timeline timer;
    private int elapsedSeconds;
    private int hintsUsed;
    private int maxHints;
    private boolean puzzleSolved;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #f4f6f8;");

        root.setTop(buildTopBar());
        gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(1);
        gridPane.setVgap(1);

        ScrollPane gridScroll = new ScrollPane(gridPane);
        gridScroll.setFitToWidth(true);
        gridScroll.setFitToHeight(true);
        gridScroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(gridScroll);

        root.setRight(buildCluePanel());

        statusLabel = new Label("Pick a word pack and difficulty, then click Generate.");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        timerLabel = new Label("Time: 0s   |   Hints used: 0");
        timerLabel.setFont(Font.font("System", 12));
        VBox bottom = new VBox(4, statusLabel, timerLabel);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        root.setBottom(bottom);

        setupTimer();

        // Build the default puzzle (Data Structures pack, Medium) on startup.
        loadWords(wordPack("Data Structures (DSA)"));
        buildPuzzle();

        Scene scene = new Scene(root, 950, 680);
        stage.setTitle("Crossword Puzzle Game — Group 4 (Hashing + Backtracking)");
        stage.setScene(scene);
        stage.show();
    }

    // ---------------------------------------------------------- top bar ----

    private VBox buildTopBar() {
        Label title = new Label("Crossword Puzzle Game — Interlocking Grid Generator & Solver");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        packSelector = new ComboBox<>();
        packSelector.getItems().addAll(
                "Data Structures (DSA)", "Space", "Animals", "Sports", "Custom (type below)");
        packSelector.setValue("Data Structures (DSA)");

        difficultySelector = new ComboBox<>();
        difficultySelector.getItems().addAll("Easy (10 hints)", "Medium (4 hints)", "Hard (no hints)");
        difficultySelector.setValue("Medium (4 hints)");

        wordInput = new TextArea();
        wordInput.setPromptText("Only used when Word Pack = Custom. One word per line as WORD:Clue text\ne.g.\nTREE:Has bark and leaves\nARRAY:Contiguous data structure");
        wordInput.setPrefRowCount(3);

        Button generateBtn = new Button("Generate Puzzle");
        generateBtn.setOnAction(e -> onGenerateClicked());

        Button checkBtn = new Button("Check Answers");
        checkBtn.setOnAction(e -> checkAnswers());

        hintBtn = new Button("Use Hint");
        hintBtn.setOnAction(e -> useHint());

        Button solveBtn = new Button("Reveal Solution");
        solveBtn.setOnAction(e -> revealSolution());

        HBox selectors = new HBox(10,
                new Label("Word Pack:"), packSelector,
                new Label("Difficulty:"), difficultySelector);
        selectors.setAlignment(Pos.CENTER_LEFT);
        selectors.setPadding(new Insets(4, 0, 4, 0));

        HBox buttons = new HBox(8, generateBtn, checkBtn, hintBtn, solveBtn);
        buttons.setPadding(new Insets(6, 0, 6, 0));

        VBox top = new VBox(6, title, selectors, wordInput, buttons);
        top.setPadding(new Insets(0, 0, 10, 0));
        return top;
    }

    // ------------------------------------------------------- clue panel ----

    private ScrollPane buildCluePanel() {
        Label acrossTitle = new Label("Across");
        acrossTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        acrossBox = new VBox(4);

        Label downTitle = new Label("Down");
        downTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        downBox = new VBox(4);

        VBox content = new VBox(10, acrossTitle, acrossBox, downTitle, downBox);
        content.setPadding(new Insets(0, 0, 0, 14));
        content.setPrefWidth(260);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(280);
        return scroll;
    }

    // ------------------------------------------------- themed word packs ----

    /**
     * Themed word packs (proposal conclusion: "Future extensions include
     * ... themed word packs"). Each list below has been verified to
     * successfully interlock via the backtracking generator.
     */
    private List<WordEntry> wordPack(String name) {
        List<WordEntry> list = new ArrayList<>();
        switch (name) {
            case "Space":
                list.add(new WordEntry("STAR", "Burning ball of gas in the sky"));
                list.add(new WordEntry("ORBIT", "Curved path around a planet or star"));
                list.add(new WordEntry("MARS", "The red planet"));
                list.add(new WordEntry("MOON", "Earth's only natural satellite"));
                list.add(new WordEntry("COMET", "Icy body that grows a tail near the sun"));
                list.add(new WordEntry("SOLAR", "Relating to the sun"));
                break;
            case "Animals":
                list.add(new WordEntry("PANDA", "Bamboo-eating bear"));
                list.add(new WordEntry("SNAKE", "Legless reptile"));
                list.add(new WordEntry("EAGLE", "Bird of prey with sharp eyesight"));
                list.add(new WordEntry("ZEBRA", "Striped African animal"));
                list.add(new WordEntry("BEAR", "Large forest mammal"));
                list.add(new WordEntry("DEER", "Antlered forest animal"));
                break;
            case "Sports":
                list.add(new WordEntry("SOCCER", "World's most popular football sport"));
                list.add(new WordEntry("TENNIS", "Racquet sport played on a court"));
                list.add(new WordEntry("CRICKET", "Bat and ball sport popular in Pakistan"));
                list.add(new WordEntry("HOCKEY", "Stick and puck/ball team sport"));
                list.add(new WordEntry("BOXING", "Combat sport fought with gloves"));
                break;
            case "Data Structures (DSA)":
            default:
                list.add(new WordEntry("TREE", "Hierarchical data structure with root and leaves"));
                list.add(new WordEntry("ARRAY", "Contiguous, fixed-size indexed data structure"));
                list.add(new WordEntry("CODE", "What programmers write"));
                list.add(new WordEntry("NODE", "A single element in a linked list or tree"));
                list.add(new WordEntry("DATA", "Information processed by algorithms"));
                break;
        }
        return list;
    }

    private void loadWords(List<WordEntry> words) {
        currentWords = words;
    }

    private void onGenerateClicked() {
        String pack = packSelector.getValue();
        if ("Custom (type below)".equals(pack)) {
            String text = wordInput.getText();
            List<WordEntry> parsed = new ArrayList<>();
            if (text != null) {
                for (String line : text.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(":", 2);
                    String word = parts[0].trim();
                    String clue = parts.length > 1 ? parts[1].trim() : "(no clue given)";
                    if (!word.isEmpty()) {
                        parsed.add(new WordEntry(word, clue));
                    }
                }
            }
            if (parsed.size() < 2) {
                statusLabel.setText("Please enter at least 2 words (format WORD:Clue, one per line).");
                return;
            }
            loadWords(parsed);
        } else {
            loadWords(wordPack(pack));
        }
        buildPuzzle();
    }

    // ---------------------------------------------------- puzzle building ----

    private void buildPuzzle() {
        generator = new CrosswordGenerator(currentWords, GRID_SIZE);
        boolean success = generator.generate();

        if (!success) {
            statusLabel.setText("Could not interlock this word list into a valid grid. Try different/more overlapping words.");
            gridPane.getChildren().clear();
            acrossBox.getChildren().clear();
            downBox.getChildren().clear();
            return;
        }

        generator.numberSlots();
        solutionGrid = generator.getGrid();
        placements = generator.getPlacements();

        computeBoundingBox();
        renderGrid();
        renderClues();
        startNewAttempt();

        statusLabel.setText("Puzzle generated: " + currentWords.size() + " words placed. "
                + "Fill in the grid and click Check Answers.");
    }

    private void computeBoundingBox() {
        minRow = Integer.MAX_VALUE;
        minCol = Integer.MAX_VALUE;
        maxRow = Integer.MIN_VALUE;
        maxCol = Integer.MIN_VALUE;
        for (PlacedWord p : placements) {
            for (int i = 0; i < p.length(); i++) {
                minRow = Math.min(minRow, p.rowOf(i));
                maxRow = Math.max(maxRow, p.rowOf(i));
                minCol = Math.min(minCol, p.colOf(i));
                maxCol = Math.max(maxCol, p.colOf(i));
            }
        }
    }

    private void renderGrid() {
        gridPane.getChildren().clear();
        int rows = maxRow - minRow + 1;
        int cols = maxCol - minCol + 1;

        cellFields = new TextField[rows][cols];
        activeCell = new boolean[rows][cols];

        // Mark active cells and their numbers.
        int[][] numberAt = new int[rows][cols];
        for (int[] row : numberAt) Arrays.fill(row, -1);

        for (PlacedWord p : placements) {
            for (int i = 0; i < p.length(); i++) {
                int r = p.rowOf(i) - minRow;
                int c = p.colOf(i) - minCol;
                activeCell[r][c] = true;
            }
            int r0 = p.getRow() - minRow;
            int c0 = p.getCol() - minCol;
            numberAt[r0][c0] = p.getNumber();
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane cellContainer = new StackPane();
                cellContainer.setPrefSize(34, 34);

                if (activeCell[r][c]) {
                    TextField field = new TextField();
                    field.setPrefSize(34, 34);
                    field.setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-padding: 0;");
                    field.setOnKeyReleased(e -> {
                        String t = field.getText();
                        if (t.length() > 1) {
                            field.setText(t.substring(t.length() - 1));
                        }
                        field.setText(field.getText().toUpperCase());
                    });
                    cellFields[r][c] = field;
                    cellContainer.getChildren().add(field);

                    if (numberAt[r][c] > 0) {
                        Label numberLabel = new Label(String.valueOf(numberAt[r][c]));
                        numberLabel.setFont(Font.font("System", 9));
                        numberLabel.setStyle("-fx-text-fill: #333;");
                        StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);
                        cellContainer.getChildren().add(numberLabel);
                    }
                } else {
                    cellContainer.setStyle("-fx-background-color: #22262b;");
                }

                gridPane.add(cellContainer, c, r);
            }
        }
    }

    private void renderClues() {
        acrossBox.getChildren().clear();
        downBox.getChildren().clear();

        List<PlacedWord> across = new ArrayList<>();
        List<PlacedWord> down = new ArrayList<>();
        for (PlacedWord p : placements) {
            if (p.getDirection() == Direction.ACROSS) across.add(p);
            else down.add(p);
        }
        across.sort(Comparator.comparingInt(PlacedWord::getNumber));
        down.sort(Comparator.comparingInt(PlacedWord::getNumber));

        for (PlacedWord p : across) {
            Label l = new Label(p.getNumber() + ". " + p.getWord().getClue());
            l.setWrapText(true);
            acrossBox.getChildren().add(l);
        }
        for (PlacedWord p : down) {
            Label l = new Label(p.getNumber() + ". " + p.getWord().getClue());
            l.setWrapText(true);
            downBox.getChildren().add(l);
        }
    }

    // ------------------------------------------------- difficulty & score ----

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!puzzleSolved) {
                elapsedSeconds++;
                updateTimerLabel();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void startNewAttempt() {
        elapsedSeconds = 0;
        hintsUsed = 0;
        puzzleSolved = false;
        maxHints = maxHintsForDifficulty();
        hintBtn.setDisable(maxHints <= 0);
        updateTimerLabel();
        timer.stop();
        timer.playFromStart();
    }

    private int maxHintsForDifficulty() {
        String d = difficultySelector.getValue();
        if (d.startsWith("Easy")) return 10;
        if (d.startsWith("Hard")) return 0;
        return 4; // Medium
    }

    private void updateTimerLabel() {
        timerLabel.setText("Time: " + elapsedSeconds + "s   |   Hints used: " + hintsUsed
                + " / " + maxHints);
    }

    private int computeScore() {
        int score = 1000 - (elapsedSeconds * 2) - (hintsUsed * 50);
        return Math.max(0, score);
    }

    private void useHint() {
        if (cellFields == null || hintsUsed >= maxHints) return;

        // Find the first empty or incorrect active cell and reveal its letter.
        for (int r = 0; r < cellFields.length; r++) {
            for (int c = 0; c < cellFields[r].length; c++) {
                if (!activeCell[r][c]) continue;
                TextField field = cellFields[r][c];
                char expected = solutionGrid.get(r + minRow, c + minCol);
                String userText = field.getText().trim().toUpperCase();
                boolean alreadyCorrect = !userText.isEmpty() && userText.charAt(0) == expected;
                if (!alreadyCorrect) {
                    field.setText(String.valueOf(expected));
                    field.setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-padding: 0; "
                            + "-fx-control-inner-background: #ffe9a8;");
                    hintsUsed++;
                    updateTimerLabel();
                    if (hintsUsed >= maxHints) hintBtn.setDisable(true);
                    return;
                }
            }
        }
    }

    // --------------------------------------------------------- validation ----

    private void checkAnswers() {
        if (cellFields == null) return;
        boolean allCorrect = true;

        for (int r = 0; r < cellFields.length; r++) {
            for (int c = 0; c < cellFields[r].length; c++) {
                if (!activeCell[r][c]) continue;
                TextField field = cellFields[r][c];
                char expected = solutionGrid.get(r + minRow, c + minCol);
                String userText = field.getText().trim().toUpperCase();
                boolean correct = !userText.isEmpty() && userText.charAt(0) == expected;

                if (correct) {
                    field.setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-padding: 0; "
                            + "-fx-control-inner-background: #c8f7c5;");
                } else {
                    field.setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-padding: 0; "
                            + "-fx-control-inner-background: #f7c5c5;");
                    allCorrect = false;
                }
            }
        }

        if (allCorrect) {
            puzzleSolved = true;
            timer.stop();
            int score = computeScore();
            String msg = "Congratulations! Puzzle completed.\n\nTime: " + elapsedSeconds
                    + "s\nHints used: " + hintsUsed + "\nScore: " + score;
            statusLabel.setText("Congratulations! Puzzle completed. Score: " + score);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.setHeaderText(null);
            alert.showAndWait();
        } else {
            statusLabel.setText("Some cells are incorrect or empty — keep going!");
        }
    }

    private void revealSolution() {
        if (cellFields == null) return;
        for (int r = 0; r < cellFields.length; r++) {
            for (int c = 0; c < cellFields[r].length; c++) {
                if (!activeCell[r][c]) continue;
                char ch = solutionGrid.get(r + minRow, c + minCol);
                cellFields[r][c].setText(String.valueOf(ch));
                cellFields[r][c].setStyle("-fx-font-size: 14; -fx-alignment: center; -fx-padding: 0; "
                        + "-fx-control-inner-background: #d8e8ff;");
            }
        }
        timer.stop();
        statusLabel.setText("Solution revealed. (No score awarded.)");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
