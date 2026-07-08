package com.dsa.crossword.ui;

import com.dsa.crossword.datastructures.HashIndexedWordBank;
import com.dsa.crossword.generator.CrosswordGenerator;
import com.dsa.crossword.model.Direction;
import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Slot;
import com.dsa.crossword.model.Word;
import com.dsa.crossword.util.PuzzleLoader;
import com.dsa.crossword.util.SolutionValidator;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crossword Puzzle Game — JavaFX GUI.
 *
 * Layout:
 *   - Left  : the crossword grid (with cell numbers and per-cell TextFields)
 *   - Right : clue list (Across / Down) + controls (timer, new puzzle, reveal, check)
 *
 * Validation:
 *   - Each cell is a TextField. We keep a 2D char[] of user input.
 *   - A cell is highlighted GREEN if it matches the solution, RED if not.
 */
public class CrosswordApp extends Application {

    // --- Model state ---
    private Grid solution;
    private char[][] userGrid;
    private List<Slot> allSlots = new ArrayList<>();
    private Map<Integer, Slot> slotByNumber = new HashMap<>();
    private Slot activeSlot = null;            // currently selected slot (highlighted)
    private List<CellView>[][] cellViews;     // UI views for each cell

    // --- UI ---
    private final GridPane gridPane = new GridPane();
    private final Label statusLabel = new Label("Welcome!");
    private final Label timerLabel = new Label("00:00");
    private final ListView<Slot> acrossList = new ListView<>();
    private final ListView<Slot> downList = new ListView<>();
    private final ObservableList<Slot> acrossData = FXCollections.observableArrayList();
    private final ObservableList<Slot> downData = FXCollections.observableArrayList();
    private final Label progressLabel = new Label("");

    // --- Game state ---
    private Timeline timeline;
    private int elapsedSeconds = 0;
    private int hintsUsed = 0;

    // -------------------------------------------------------------------------
    // Application entry point
    // -------------------------------------------------------------------------
    @Override
    public void start(Stage stage) {
        stage.setTitle("Crossword Puzzle Game — DSA Project");

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildGridContainer());
        root.setRight(buildCluePanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(e -> { if (timeline != null) timeline.stop(); });
        stage.show();

        loadAndGenerate("puzzles/sample1.txt");
    }

    // -------------------------------------------------------------------------
    // Header (title + timer + controls)
    // -------------------------------------------------------------------------
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        Label title = new Label("CROSSWORD");
        title.getStyleClass().add("title");

        timerLabel.getStyleClass().add("timer");

        Button newBtn = new Button("New Puzzle");
        newBtn.setOnAction(e -> showPuzzleChooser());

        Button checkBtn = new Button("Check");
        checkBtn.setOnAction(e -> checkAll());

        Button revealBtn = new Button("Reveal");
        revealBtn.setOnAction(e -> revealAll());

        Button hintBtn = new Button("Hint (Reveal Letter)");
        hintBtn.setOnAction(e -> revealOneLetter());

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> clearAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, timerLabel, newBtn, checkBtn, hintBtn, revealBtn, clearBtn);
        return header;
    }

    // -------------------------------------------------------------------------
    // Grid container
    // -------------------------------------------------------------------------
    private ScrollPane buildGridContainer() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER);
        progressLabel.getStyleClass().add("progress");
        box.getChildren().addAll(progressLabel, gridPane);
        gridPane.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("grid-scroll");
        return scroll;
    }

    // -------------------------------------------------------------------------
    // Right-hand clue panel
    // -------------------------------------------------------------------------
    private VBox buildCluePanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(420);
        panel.setMinWidth(300);
        panel.getStyleClass().add("clue-panel");

        Label acrossHeader = new Label("Across");
        acrossHeader.getStyleClass().add("clue-header");
        acrossList.setItems(acrossData);
        acrossList.setCellFactory(lv -> new ClueCell());
        acrossList.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) {
                activeSlot = sel;
                focusActiveSlot(0);
                refreshClueSelection();
            }
        });

        Label downHeader = new Label("Down");
        downHeader.getStyleClass().add("clue-header");
        downList.setItems(downData);
        downList.setCellFactory(lv -> new ClueCell());
        downList.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) {
                activeSlot = sel;
                focusActiveSlot(0);
                refreshClueSelection();
            }
        });

        panel.getChildren().addAll(acrossHeader, acrossList, downHeader, downList);
        VBox.setVgrow(acrossList, Priority.ALWAYS);
        VBox.setVgrow(downList, Priority.ALWAYS);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Status bar
    // -------------------------------------------------------------------------
    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.getStyleClass().add("statusbar");
        statusLabel.getStyleClass().add("status");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    // -------------------------------------------------------------------------
    // Puzzle loading + generation
    // -------------------------------------------------------------------------
    private void loadAndGenerate(String resourcePath) {
        try {
            List<Word> words = PuzzleLoader.fromResource("/" + resourcePath);
            runGeneration(words, resourcePath);
        } catch (IOException ex) {
            showError("Could not load puzzle: " + ex.getMessage());
        }
    }

    private void loadAndGenerateFromFile(Path file) {
        try {
            List<Word> words = PuzzleLoader.fromFile(file);
            runGeneration(words, file.getFileName().toString());
        } catch (IOException ex) {
            showError("Could not load file: " + ex.getMessage());
        }
    }

    private void runGeneration(List<Word> words, String sourceLabel) {
        if (words.size() < 3) {
            showError("Need at least 3 words to build a crossword.");
            return;
        }

        statusLabel.setText("Indexing word bank...");
        HashIndexedWordBank bank = new HashIndexedWordBank();
        bank.addAll(words);
        statusLabel.setText("Hash-indexed " + bank.totalWords() + " words. Backtracking...");

        CrosswordGenerator gen = new CrosswordGenerator(bank);
        CrosswordGenerator.GenerationResult res = gen.generate();

        if (!res.success) {
            showError("Generation failed: " + res.message + " (source: " + sourceLabel + ")");
            return;
        }

        this.solution = res.grid;
        this.allSlots = res.slots;
        this.activeSlot = res.anchorSlot;

        rebuildSlotMap();
        rebuildUserGrid();
        buildGridUI();
        buildClueLists();
        resetTimer();
        startTimer();
        updateProgress();

        statusLabel.setText("Puzzle ready: " + sourceLabel + "  |  words placed: "
                + allSlots.size() + "  |  backtrack steps: " + res.backtrackSteps);
    }

    private void rebuildSlotMap() {
        slotByNumber.clear();
        for (Slot s : allSlots) slotByNumber.put(s.getNumber(), s);
        if (allSlots.size() > 0) {
            activeSlot = allSlots.get(0);
        }
    }

    private void rebuildUserGrid() {
        userGrid = new char[solution.rows()][solution.cols()];
        for (int r = 0; r < solution.rows(); r++)
            for (int c = 0; c < solution.cols(); c++)
                userGrid[r][c] = ' ';
    }

    // -------------------------------------------------------------------------
    // Build the grid UI
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void buildGridUI() {
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();

        int rows = solution.rows();
        int cols = solution.cols();

        cellViews = new List[rows][cols];

        double cellSize = Math.max(30, Math.min(38, 760.0 / Math.max(rows, cols)));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(cellSize, cellSize);
                cell.setMinSize(cellSize, cellSize);
                cell.setMaxSize(cellSize, cellSize);
                cell.getStyleClass().add("grid-cell");

                if (solution.get(r, c) != Grid.BLOCKED) {
                    TextField tf = new TextField();
                    tf.setPrefSize(cellSize, cellSize);
                    tf.setMinSize(cellSize, cellSize);
                    tf.setMaxSize(cellSize, cellSize);
                    tf.getStyleClass().add("cell-input");
                    tf.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(16, cellSize * 0.46)));
                    tf.setAlignment(Pos.CENTER);
                    tf.setDisable(false);

                    final int rr = r, cc = c;
                    tf.textProperty().addListener((o, old, val) -> {
                        if (val == null) return;
                        String v = val.toUpperCase();
                        if (v.length() > 1) {
                            tf.setText(v.substring(v.length() - 1));
                            return;
                        }
                        if (!v.isEmpty() && !Character.isLetter(v.charAt(0))) {
                            tf.setText("");
                            return;
                        }
                        userGrid[rr][cc] = v.isEmpty() ? ' ' : v.charAt(0);
                        tf.setText(v);
                        validateCell(rr, cc);
                        updateProgress();
                        if (!v.isEmpty()) advanceFocus(rr, cc);
                    });

                    tf.setOnKeyPressed((KeyEvent e) -> {
                        switch (e.getCode()) {
                            case BACK_SPACE:
                                if (tf.getText().isEmpty()) moveFocus(rr, cc, -1);
                                break;
                            case LEFT:  moveFocus(rr, cc, -1); break;
                            case RIGHT: moveFocus(rr, cc, +1); break;
                            case UP:    moveFocus(rr, cc, -2); break;
                            case DOWN:  moveFocus(rr, cc, +2); break;
                            default: break;
                        }
                    });

                    tf.setOnMouseClicked(e -> {
                        // Picking a cell sets the active slot if possible
                        Slot s = findSlotContaining(rr, cc);
                        if (s != null) {
                            activeSlot = s;
                            refreshClueSelection();
                            refreshHighlights();
                        }
                    });

                    cell.getChildren().add(tf);

                    // Add number label if any slot starts here
                    int num = numberAt(r, c);
                    if (num > 0) {
                        Label numLbl = new Label(String.valueOf(num));
                        numLbl.getStyleClass().add("cell-number");
                        StackPane.setAlignment(numLbl, Pos.TOP_LEFT);
                        StackPane.setMargin(numLbl, new Insets(1, 0, 0, 3));
                        cell.getChildren().add(numLbl);
                    }

                    cellViews[r][c] = new ArrayList<>();
                    cellViews[r][c].add(new CellView(cell, tf));
                } else {
                    cell.getStyleClass().add("grid-cell-blocked");
                }

                gridPane.add(cell, c, r);
            }
        }

        refreshHighlights();
    }

    /** Return the crossword number of the slot that starts at (r, c), or 0. */
    private int numberAt(int r, int c) {
        for (Slot s : allSlots) {
            if (s.getRow() == r && s.getCol() == c) return s.getNumber();
        }
        return 0;
    }

    /** Find a slot that includes the given cell, preferring the active direction. */
    private Slot findSlotContaining(int r, int c) {
        Slot across = null, down = null;
        for (Slot s : allSlots) {
            if (!slotContains(s, r, c)) continue;
            if (s.getDirection() == Direction.ACROSS) across = s;
            else                                      down    = s;
        }
        if (activeSlot != null && activeSlot.getDirection() == Direction.ACROSS && across != null) return across;
        if (activeSlot != null && activeSlot.getDirection() == Direction.DOWN   && down    != null) return down;
        if (across != null) return across;
        return down;
    }

    private boolean slotContains(Slot s, int r, int c) {
        for (int i = 0; i < s.getLength(); i++) {
            int[] cell = s.cellAt(i);
            if (cell[0] == r && cell[1] == c) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Focus & highlighting helpers
    // -------------------------------------------------------------------------
    private void focusActiveSlot(int offset) {
        if (activeSlot == null) return;
        int[] first = activeSlot.cellAt(Math.max(0, Math.min(offset, activeSlot.getLength() - 1)));
        focusCell(first[0], first[1]);
    }

    private void focusCell(int r, int c) {
        if (cellViews == null) return;
        if (r < 0 || r >= solution.rows() || c < 0 || c >= solution.cols()) return;
        if (cellViews[r][c] == null) return;
        CellView v = cellViews[r][c].get(0);
        v.input.requestFocus();
        v.input.selectAll();
    }

    private void advanceFocus(int r, int c) {
        if (activeSlot == null) return;
        // Find current index in slot
        int idx = 0;
        for (int i = 0; i < activeSlot.getLength(); i++) {
            int[] cell = activeSlot.cellAt(i);
            if (cell[0] == r && cell[1] == c) { idx = i; break; }
        }
        if (idx + 1 < activeSlot.getLength()) {
            int[] nxt = activeSlot.cellAt(idx + 1);
            focusCell(nxt[0], nxt[1]);
        }
    }

    /** Move focus with arrow keys, constrained to the active slot. */
    private void moveFocus(int r, int c, int dir) {
        if (activeSlot == null) return;
        int idx = 0;
        for (int i = 0; i < activeSlot.getLength(); i++) {
            int[] cell = activeSlot.cellAt(i);
            if (cell[0] == r && cell[1] == c) { idx = i; break; }
        }
        int newIdx = idx;
        if (dir == -1 || dir == +1) {
            if (activeSlot.getDirection() == Direction.ACROSS) newIdx = idx + dir;
        } else {
            // vertical movement jumps slot
            Slot other = findPerpendicularSlotAt(r, c, activeSlot.getDirection().opposite());
            if (other != null) {
                activeSlot = other;
                refreshClueSelection();
                refreshHighlights();
                int[] cell = other.cellAt(Math.min(idx, other.getLength() - 1));
                focusCell(cell[0], cell[1]);
                return;
            }
        }
        if (newIdx >= 0 && newIdx < activeSlot.getLength()) {
            int[] nxt = activeSlot.cellAt(newIdx);
            focusCell(nxt[0], nxt[1]);
        }
    }

    private Slot findPerpendicularSlotAt(int r, int c, Direction dir) {
        for (Slot s : allSlots) {
            if (s.getDirection() != dir) continue;
            for (int i = 0; i < s.getLength(); i++) {
                int[] cell = s.cellAt(i);
                if (cell[0] == r && cell[1] == c) return s;
            }
        }
        return null;
    }

    private void refreshClueSelection() {
        if (activeSlot == null) return;
        if (activeSlot.getDirection() == Direction.ACROSS) {
            acrossList.getSelectionModel().select(activeSlot);
            downList.getSelectionModel().clearSelection();
        } else {
            downList.getSelectionModel().select(activeSlot);
            acrossList.getSelectionModel().clearSelection();
        }
    }

    private void refreshHighlights() {
        if (cellViews == null || activeSlot == null) return;
        for (int r = 0; r < solution.rows(); r++) {
            for (int c = 0; c < solution.cols(); c++) {
                if (cellViews[r][c] == null) continue;
                CellView v = cellViews[r][c].get(0);
                v.pane.getStyleClass().removeAll("cell-active", "cell-highlight");
                if (slotContains(activeSlot, r, c)) {
                    v.pane.getStyleClass().add("cell-highlight");
                }
            }
        }
        // Highlight the current cell
        for (int i = 0; i < activeSlot.getLength(); i++) {
            int[] cell = activeSlot.cellAt(i);
            if (cellViews[cell[0]][cell[1]] == null) continue;
            cellViews[cell[0]][cell[1]].get(0).pane.getStyleClass().add("cell-active");
        }
    }

    // -------------------------------------------------------------------------
    // Validation & progress
    // -------------------------------------------------------------------------
    private void validateCell(int r, int c) {
        if (cellViews == null || cellViews[r][c] == null) return;
        CellView v = cellViews[r][c].get(0);
        v.pane.getStyleClass().removeAll("cell-correct", "cell-wrong");
        if (userGrid[r][c] == ' ') return;
        if (SolutionValidator.isCellCorrect(solution, userGrid, r, c)) {
            v.pane.getStyleClass().add("cell-correct");
        } else {
            v.pane.getStyleClass().add("cell-wrong");
        }
    }

    private void checkAll() {
        int correct = SolutionValidator.countCorrect(solution, userGrid);
        int total = SolutionValidator.countOpen(solution);
        statusLabel.setText("Checked: " + correct + " / " + total + " cells correct.");
    }

    private void revealAll() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Reveal the full solution?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                for (int r = 0; r < solution.rows(); r++) {
                    for (int c = 0; c < solution.cols(); c++) {
                        if (solution.get(r, c) != Grid.BLOCKED) {
                            userGrid[r][c] = solution.get(r, c);
                            CellView v = cellViews[r][c].get(0);
                            v.input.setText(String.valueOf(solution.get(r, c)));
                            validateCell(r, c);
                        }
                    }
                }
                statusLabel.setText("Solution revealed.");
            }
        });
    }

    private void revealOneLetter() {
        if (activeSlot == null || activeSlot.getWord() == null) return;
        // Find the first empty cell in the active slot
        for (int i = 0; i < activeSlot.getLength(); i++) {
            int[] cell = activeSlot.cellAt(i);
            if (userGrid[cell[0]][cell[1]] == ' ') {
                userGrid[cell[0]][cell[1]] = activeSlot.getWord().charAt(i);
                CellView v = cellViews[cell[0]][cell[1]].get(0);
                v.input.setText(String.valueOf(activeSlot.getWord().charAt(i)));
                validateCell(cell[0], cell[1]);
                hintsUsed++;
                updateProgress();
                statusLabel.setText("Hint used (" + hintsUsed + " total).");
                return;
            }
        }
        statusLabel.setText("No empty cell in this slot.");
    }

    private void clearAll() {
        if (solution == null) return;
        for (int r = 0; r < solution.rows(); r++) {
            for (int c = 0; c < solution.cols(); c++) {
                if (solution.get(r, c) != Grid.BLOCKED) {
                    userGrid[r][c] = ' ';
                    CellView v = cellViews[r][c].get(0);
                    v.input.setText("");
                    v.pane.getStyleClass().removeAll("cell-correct", "cell-wrong");
                }
            }
        }
        statusLabel.setText("Cleared.");
        updateProgress();
    }

    private void updateProgress() {
        if (solution == null) return;
        int correct = 0;
        int open = 0;
        for (int r = 0; r < solution.rows(); r++) {
            for (int c = 0; c < solution.cols(); c++) {
                if (solution.get(r, c) == Grid.BLOCKED) continue;
                open++;
                if (userGrid[r][c] != ' '
                        && Character.toUpperCase(userGrid[r][c]) == solution.get(r, c)) {
                    correct++;
                }
            }
        }
        progressLabel.setText("Progress: " + correct + " / " + open + " cells");
        if (correct == open && open > 0) {
            onPuzzleSolved();
        }
    }

    private void onPuzzleSolved() {
        if (timeline != null) timeline.stop();
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "You solved the puzzle in " + minutes + "m " + seconds + "s, with " + hintsUsed + " hint(s).");
        a.setHeaderText("Congratulations!");
        a.setTitle("Puzzle Complete");
        a.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Clue lists
    // -------------------------------------------------------------------------
    private void buildClueLists() {
        acrossData.clear();
        downData.clear();
        for (Slot s : allSlots) {
            if (s.getDirection() == Direction.ACROSS) acrossData.add(s);
            else                                      downData.add(s);
        }
        if (activeSlot != null) {
            acrossList.getSelectionModel().select(activeSlot);
            downList.getSelectionModel().select(activeSlot);
        }
    }

    // -------------------------------------------------------------------------
    // Timer
    // -------------------------------------------------------------------------
    private void resetTimer() {
        if (timeline != null) timeline.stop();
        elapsedSeconds = 0;
        hintsUsed = 0;
        timerLabel.setText("00:00");
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            int m = elapsedSeconds / 60;
            int s = elapsedSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // -------------------------------------------------------------------------
    // Puzzle chooser
    // -------------------------------------------------------------------------
    private void showPuzzleChooser() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Built-in 1 (DSA)",
                "Built-in 1 (DSA)", "Built-in 2 (Java)", "Built-in 3 (Pakistan)",
                "Load .txt from disk...");
        dialog.setTitle("New Puzzle");
        dialog.setHeaderText("Choose a puzzle to generate:");
        dialog.setContentText("Puzzle:");
        dialog.showAndWait().ifPresent(choice -> {
            switch (choice) {
                case "Built-in 1 (DSA)":      loadAndGenerate("puzzles/sample1.txt"); break;
                case "Built-in 2 (Java)":     loadAndGenerate("puzzles/sample2.txt"); break;
                case "Built-in 3 (Pakistan)": loadAndGenerate("puzzles/sample3.txt"); break;
                case "Load .txt from disk...": pickFile(); break;
            }
        });
    }

    private void pickFile() {
        // Use a simple TextInputDialog asking for a path, since
        // FileChooser requires extra setup; we still support it if available.
        TextInputDialog dlg = new TextInputDialog("puzzles/custom.txt");
        dlg.setTitle("Load .txt puzzle");
        dlg.setHeaderText("Enter path to a .txt file (relative or absolute):");
        dlg.setContentText("Path:");
        dlg.showAndWait().ifPresent(p -> {
            Path path = Paths.get(p);
            if (!path.isAbsolute()) path = Paths.get("src/main/resources", p);
            loadAndGenerateFromFile(path);
        });
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------
    private void showError(String msg) {
        statusLabel.setText("Error: " + msg);
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // -------------------------------------------------------------------------
    // Helper records
    // -------------------------------------------------------------------------
    private static final class CellView {
        final StackPane pane;
        final TextField input;
        CellView(StackPane pane, TextField input) { this.pane = pane; this.input = input; }
    }

    /** Custom cell renderer for the clue list. */
    private static final class ClueCell extends ListCell<Slot> {
        ClueCell() {
            setWrapText(true);
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(Slot s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) {
                setText(null);
            } else {
                Word w = s.getWord();
                String text = (w != null ? w.getText() : "????");
                setText(s.getNumber() + ". " + (s.getDirection() == Direction.ACROSS ? "[" + text + "]  " : "")
                        + (w != null ? w.getClue() : "(no clue)"));
            }
        }
    }

    // (silence unused warning for Platform import)
    @SuppressWarnings("unused")
    private static void _unused() { Platform.exit(); }
}
