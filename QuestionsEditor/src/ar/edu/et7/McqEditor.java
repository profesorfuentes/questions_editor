package ar.edu.et7;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class McqEditor extends JFrame {
    private static final int MAX_CHOICES = 10; // Cantidad máxima de choices

    private JTextField txtTitle, txtCategory, txtPoints;
    private JTextArea txtStimulus, txtPrompt;
    private JTextField[] txtChoices = new JTextField[MAX_CHOICES];
    private JCheckBox[] chkRightAnswer = new JCheckBox[MAX_CHOICES];
    private JLabel[] lblPointsPerChoice = new JLabel[MAX_CHOICES];  // Etiqueta para mostrar el puntaje por Choice
    private JButton btnSave, btnNext, btnInsert, btnDelete, btnPrevious;
    private JFileChooser fileChooser;
    private ArrayNode questionsArray;
    private int currentIndex = -1;
    private File currentFile;
    private ObjectMapper objectMapper;
    private DecimalFormat df = new DecimalFormat("#.00");  // Formato para 2 decimales
    private JLabel lblQuestionIndex;  // Nueva etiqueta para mostrar n/t (índice y total)

    public McqEditor() {
        objectMapper = new ObjectMapper();

        setTitle("MCQ Editor");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior para mostrar el índice de la pregunta
        JPanel topPanel = new JPanel(new BorderLayout());
        lblQuestionIndex = new JLabel("Question 0/0      ", SwingConstants.RIGHT);
        topPanel.add(lblQuestionIndex, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);  // Agregar el panel superior al marco principal

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        txtTitle = new JTextField();
        formPanel.add(txtTitle, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        txtCategory = new JTextField();
        formPanel.add(txtCategory, gbc);

        // Stimulus
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Stimulus:"), gbc);
        gbc.gridx = 1;
        txtStimulus = new JTextArea(8, 20);
        JScrollPane scrollStimulus = new JScrollPane(txtStimulus);
        scrollStimulus.setPreferredSize(new Dimension(200, 150));
        formPanel.add(scrollStimulus, gbc);

        // Prompt
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Prompt:"), gbc);
        gbc.gridx = 1;
        txtPrompt = new JTextArea(8, 20);
        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setPreferredSize(new Dimension(200, 150));
        formPanel.add(scrollPrompt, gbc);

        // Points
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Points:"), gbc);
        gbc.gridx = 1;
        txtPoints = new JTextField();
        formPanel.add(txtPoints, gbc);

        // Choices
        for (int i = 0; i < MAX_CHOICES; i++) {
            gbc.gridx = 0;
            gbc.gridy = 5 + i;
            formPanel.add(new JLabel("Choice " + (char) ('A' + i) + ":"), gbc);
            
            gbc.gridx = 1;
            txtChoices[i] = new JTextField();
            formPanel.add(txtChoices[i], gbc);

            // Checkbox for Right Answer
            gbc.gridx = 2;
            chkRightAnswer[i] = new JCheckBox("Right Answer");
            chkRightAnswer[i].addActionListener(e -> {
                updateChoicePoints();  // Actualizar puntos
                updateChoiceHighlights();  // Resaltar la opción
            });
            formPanel.add(chkRightAnswer[i], gbc);

            // Label for points per choice
            gbc.gridx = 3;
            lblPointsPerChoice[i] = new JLabel("0.00");
            formPanel.add(lblPointsPerChoice[i], gbc);
        }

        // Crear botones
        JPanel buttonPanel = new JPanel();
        btnSave = new JButton("Save Question");
        btnNext = new JButton("Next Question");
        btnPrevious = new JButton("Previous Question"); 
        btnInsert = new JButton("Insert Question");
        btnDelete = new JButton("Delete Question");

        buttonPanel.add(btnSave);
        buttonPanel.add(btnPrevious);
        buttonPanel.add(btnNext);
        buttonPanel.add(btnInsert);
        buttonPanel.add(btnDelete);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("File");

        JMenuItem menuOpen = new JMenuItem("Open");
        JMenuItem menuSave = new JMenuItem("Save");
        JMenuItem menuSaveAs = new JMenuItem("Save As...");

        menuFile.add(menuOpen);
        menuFile.add(menuSave);
        menuFile.add(menuSaveAs);
        menuBar.add(menuFile);
        setJMenuBar(menuBar);

        // Action listeners
        menuOpen.addActionListener(e -> openFile());
        menuSave.addActionListener(e -> saveFile(false));
        menuSaveAs.addActionListener(e -> saveFile(true));
        btnSave.addActionListener(e -> saveCurrentQuestion());
        btnNext.addActionListener(e -> nextQuestion());
        btnPrevious.addActionListener(e -> previousQuestion());
        btnInsert.addActionListener(e -> insertNewQuestion());
        btnDelete.addActionListener(e -> deleteCurrentQuestion());
        txtPoints.addActionListener(e -> updateChoicePoints());  // Update when points change
    }

    // Método para actualizar los puntajes de los choices
    private void updateChoicePoints() {
        try {
            double totalPoints = Double.parseDouble(txtPoints.getText());
            int rightAnswersCount = 0;

            // Contar cuántos choices están marcados como Right Answer
            for (JCheckBox chk : chkRightAnswer) {
                if (chk.isSelected()) {
                    rightAnswersCount++;
                }
            }

            if (rightAnswersCount > 0) {
                // Calcular el puntaje por choice
                double basePointsPerChoice = totalPoints / rightAnswersCount;
                double sumOfPoints = 0;

                for (int i = 0; i < chkRightAnswer.length; i++) {
                    if (chkRightAnswer[i].isSelected()) {
                        // Aplicar el formato a 2 decimales
                        double choicePoints = Double.parseDouble(df.format(basePointsPerChoice));
                        lblPointsPerChoice[i].setText(df.format(choicePoints));
                        sumOfPoints += choicePoints;
                    } else {
                        lblPointsPerChoice[i].setText("0.00");
                    }
                }

                // Ajustar el primer choice si hay diferencia
                double difference = totalPoints - sumOfPoints;
                if (Math.abs(difference) > 0.001 && chkRightAnswer[0].isSelected()) {
                    double adjustedPoints = basePointsPerChoice + difference;
                    lblPointsPerChoice[0].setText(df.format(adjustedPoints));
                }
            } else {
                // Si no hay respuestas correctas seleccionadas
                for (JLabel lbl : lblPointsPerChoice) {
                    lbl.setText("0.00");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format in Points field", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Método para actualizar el resaltado de los choices seleccionados
    private void updateChoiceHighlights() {
        for (int i = 0; i < MAX_CHOICES; i++) {
            if (chkRightAnswer[i].isSelected()) {
                txtChoices[i].setBackground(Color.YELLOW);  // Resaltar en azul
            } else {
                txtChoices[i].setBackground(Color.WHITE);  // Fondo blanco para no seleccionados
            }
        }
    }
    

    // Método para actualizar el índice de la pregunta (n/t)
    private void updateQuestionIndexLabel() {
        if (questionsArray != null) {
            lblQuestionIndex.setText("Question " + (currentIndex + 1) + "/" + questionsArray.size() + "      ");
        } else {
            lblQuestionIndex.setText("Question 0/0      ");
        }
    }
    
    // Método para abrir un archivo JSON
    private void openFile() {
        fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try {
                JsonNode rootNode = objectMapper.readTree(currentFile);
                if (rootNode.isArray()) {
                    questionsArray = (ArrayNode) rootNode;
                } else {
                    JOptionPane.showMessageDialog(this, "The file format is incorrect. Expected an array of questions.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (questionsArray.size() > 0) {
                    currentIndex = 0;
                    loadQuestion(currentIndex);
                } else {
                    JOptionPane.showMessageDialog(this, "The file is empty.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Asegurar que questionsArray esté inicializado si el archivo está vacío
        if (questionsArray == null) {
            questionsArray = objectMapper.createArrayNode();
        }
    }
    
    
    // Método para guardar el archivo JSON
    private void saveFile(boolean saveAs) {
        if (currentFile == null || saveAs) {
            fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            }
        }

        if (currentFile != null) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(currentFile, questionsArray);
                JOptionPane.showMessageDialog(this, "File saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

 // Método para cargar una pregunta del JSON
    private void loadQuestion(int index) {
        if (index >= 0 && index < questionsArray.size()) {
            JsonNode question = questionsArray.get(index);
            txtTitle.setText(question.get("title").asText());
            txtCategory.setText(question.get("category").asText());
            txtStimulus.setText(question.get("stimulus").asText());
            txtPrompt.setText(question.get("prompt").asText());
            txtPoints.setText(String.valueOf(question.get("points").asDouble()));

            // Limpiar los campos de respuesta antes de cargarlos
            for (int i = 0; i < MAX_CHOICES; i++) {
                chkRightAnswer[i].setSelected(false);
                txtChoices[i].setText("");
            }

            ArrayNode choices = (ArrayNode) question.get("choices");
            for (int i = 0; i < choices.size(); i++) {
                txtChoices[i].setText(choices.get(i).get("content").asText());
            }

            // Cargar las respuestas correctas desde un array de arrays
            ArrayNode answers = (ArrayNode) question.get("answers");
            for (JsonNode answerArray : answers) {
                for (JsonNode answer : answerArray) {
                    for (int i = 0; i < choices.size(); i++) {
                        if (answer.asText().equals(choices.get(i).get("id").asText())) {
                            chkRightAnswer[i].setSelected(true);
                        }
                    }
                }
            }

            updateChoicePoints();
            updateChoiceHighlights();
            updateQuestionIndexLabel();
        }
    }

    // Método para guardar la pregunta actual en el JSON
    private void saveCurrentQuestion() {
        if (currentIndex >= 0 && currentIndex < questionsArray.size()) {
            ObjectNode question = (ObjectNode) questionsArray.get(currentIndex);
            question.put("title", txtTitle.getText());
            question.put("category", txtCategory.getText());
            question.put("stimulus", txtStimulus.getText());
            question.put("prompt", txtPrompt.getText());
            question.put("points", Double.parseDouble(txtPoints.getText()));

            ArrayNode choices = objectMapper.createArrayNode();
            ArrayNode answers = objectMapper.createArrayNode();  // Array de arrays

            // Iterar sobre los choices y guardar las respuestas correctas
            for (int i = 0; i < MAX_CHOICES; i++) {
                if (!txtChoices[i].getText().isEmpty()) {
                    ObjectNode choice = objectMapper.createObjectNode();
                    choice.put("id", UUID.randomUUID().toString());
                    choice.put("content", txtChoices[i].getText());
                    choices.add(choice);

                    if (chkRightAnswer[i].isSelected()) {
                        ArrayNode answerArray = objectMapper.createArrayNode();
                        answerArray.add(choice.get("id"));
                        answers.add(answerArray);
                    }
                }
            }

            question.set("choices", choices);
            question.set("answers", answers);
        }
    }

    
    // Utilidad para convertir ArrayNode a lista de Strings
    private java.util.List<String> getArrayAsStringList(ArrayNode arrayNode) {
        java.util.List<String> list = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            list.add(node.asText());
        }
        return list;
    }
    
    // Método para eliminar la pregunta actual
    private void deleteCurrentQuestion() {
        if (currentIndex >= 0 && currentIndex < questionsArray.size()) {
            questionsArray.remove(currentIndex);  // Remover la pregunta actual
            if (questionsArray.size() > 0) {
                // Si hay más preguntas, cargamos la anterior o la siguiente
                if (currentIndex > 0) {
                    currentIndex--;
                }
                loadQuestion(currentIndex);  // Cargar la siguiente pregunta
            } else {
                // Si no hay preguntas, limpiamos el formulario
                clearForm();
            }
        } else {
            JOptionPane.showMessageDialog(this, "This is the first question.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // Método para limpiar el formulario cuando no hay preguntas
    private void clearForm() {
        txtTitle.setText("");
        txtCategory.setText("");
        txtStimulus.setText("");
        txtPrompt.setText("");
        txtPoints.setText("");
        for (int i = 0; i < 4; i++) {
            txtChoices[i].setText("");
        }
        //txtAnswers.setText("");
    }
    
    
    // Métodos nextQuestion, previousQuestion y insertNewQuestion ahora actualizan el índice
    private void nextQuestion() {
        if (questionsArray == null || questionsArray.size() == 0) {
            JOptionPane.showMessageDialog(this, "No questions available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (currentIndex < questionsArray.size() - 1) {
            saveCurrentQuestion();  // Guardar la pregunta actual antes de pasar a la siguiente
            currentIndex++;  // Incrementar el índice
            loadQuestion(currentIndex);  // Cargar la siguiente pregunta en el formulario
        } else {
            JOptionPane.showMessageDialog(this, "You are at the last question.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void previousQuestion() {
        if (questionsArray == null || questionsArray.size() == 0) {
            JOptionPane.showMessageDialog(this, "No questions available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (currentIndex > 0) {
            saveCurrentQuestion();  // Guardar la pregunta actual antes de pasar a la anterior
            currentIndex--;  // Decrementar el índice
            loadQuestion(currentIndex);  // Cargar la pregunta anterior en el formulario
        } else {
            JOptionPane.showMessageDialog(this, "You are at the first question.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void insertNewQuestion() {
        ObjectNode newQuestion = objectMapper.createObjectNode();
        newQuestion.put("title", "");
        newQuestion.put("category", "");
        newQuestion.put("stimulus", "");
        newQuestion.put("prompt", "");
        newQuestion.put("points", 0.0);

        ArrayNode choices = objectMapper.createArrayNode();
        for (int i = 0; i < MAX_CHOICES; i++) {
            ObjectNode choice = objectMapper.createObjectNode();
            choice.put("id", UUID.randomUUID().toString());
            choice.put("content", "");
            choices.add(choice);
        }
        newQuestion.set("choices", choices);
        newQuestion.set("answers", objectMapper.createArrayNode());

        if (questionsArray == null) {
            questionsArray = objectMapper.createArrayNode();
        }

        // Insertar la nueva pregunta en la lista
        currentIndex++;
        questionsArray.insert(currentIndex, newQuestion);
        loadQuestion(currentIndex);  // Cargar la nueva pregunta
        updateQuestionIndexLabel();  // Actualizar el índice
    }

    // Otros métodos se mantienen iguales...

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new McqEditor().setVisible(true);
        });
    }
}
