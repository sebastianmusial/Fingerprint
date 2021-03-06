package fingerprint.test;

import fingerprint.linefinder.LineFinder;
import fingerprint.linefinder.LineParams;
import fingerprint.linefinder.LineResult;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Controller {
	@FXML
	BorderPane root;
	@FXML
	MenuItem mniFileOpen, mniFileExit;
	@FXML
	Button btnApplyFilter, btnAcceptFilter, btnSaveFingerprint, btnCheckLines, btnFind;
	@FXML
	ImageView imgLeft, imgRight;
	@FXML
	ComboBox<Filter> filterChooser;
    @FXML
    TextField inpFingerprintName;
    @FXML
    TextArea resultsArea;
    @FXML
    Label resultsLabel;

	LazyLoad<FileChooser> fileChooserSupplier = new LazyLoad().withSupplier(() -> new FileChooser());
	Filters filters = Filters.getFilters();
	FileChooser.ExtensionFilter extensionFilter;
	LineFinder lineFinder;
	LineParams lineParams;
    List<FingerprintData> fingerprints = new ArrayList<>();
    List<FingerprintData> compareData = new ArrayList<>();

    Map<Integer, Integer> horizontalLines = null;
    Map<Integer, Integer> verticalLines = null;

    {
		lineFinder = new LineFinder();
		lineParams = new LineParams();
		lineParams.horizontalIndexes = new int[] {33, 50, 67};
		lineParams.verticalIndexes = lineParams.horizontalIndexes;
		lineParams.unit = LineParams.Unit.PERCENTAGE;
        readData();
	}

    void readData() {
        //odczyt z pliku
        BufferedReader reader;
        try {
            String currentLine;
            reader = new BufferedReader(new FileReader("data.txt"));
            while ((currentLine = reader.readLine()) != null) {
                char firstLetter = currentLine.charAt(0);
                if(firstLetter == '#') {
                    FingerprintData newFingerprint = new FingerprintData();
                    fingerprints.add(newFingerprint);
                    String name = currentLine.substring(1, currentLine.length());
                    newFingerprint.setName(name);
                    int i = 0;
                    String data;

                    while(i++ < 6 && (data = reader.readLine()) != null) {
                        char symbol = data.charAt(0);
                        char cIndex = data.charAt(1);
                        int index = Character.getNumericValue(cIndex);
                        String svalue = data.substring(3, data.length());
                        int value = Integer.parseInt(svalue);

                        if(symbol == 'V') {
                            newFingerprint.setVerticalData(index, value);
                        }
                        else if(symbol == 'H') {
                            newFingerprint.setHorizontalData(index, value);
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //wypisanie danych
        for (FingerprintData fingerprint : fingerprints) {
            System.out.println("#" + fingerprint.getName());
            for (int i = 0; i < fingerprint.getHorizontalData().length; i++) {
                System.out.println("H" + fingerprint.getHorizontalData(i));
            }
            for (int i = 0; i < fingerprint.getVerticalData().length; i++) {
                System.out.println("V" + fingerprint.getVerticalData(i));
            }
        }
    }

	@FXML
	void initialize() {
		enableButtons(false);
		for (Filter fn : filters) {
			filterChooser.getItems().add(fn);
		}
		filterChooser.setValue(filters.get(0));
		extensionFilter = new FileChooser.ExtensionFilter("Obrazki", ".jpg", ".jpeg", ".bmp", ".png");
	}

	@FXML
	void handleApplyFilter() {
		filterChooser
				.getValue()
				.withImage(imgLeft.getImage())
				.filter()
				.setImage(imgRight);
	}

    @FXML
    void handleAcceptFilter() {
        imgLeft.setImage(imgRight.getImage());
        imgRight.setImage(null);
    }

	@FXML
	void handleCheckLines() {
        //TODO: zrobić aby użyło filtru szukania odcisku nawet jeżeli jest inny wybrany
        handleApplyFilter();
        handleAcceptFilter();

		System.out.println("Image size: " + (int)imgLeft.getImage().getWidth() + "x" + (int)imgLeft.getImage().getHeight());
        LineResult result = lineFinder
                .image(imgLeft.getImage())
                .params(lineParams)
                .find()
                .getResult();

        horizontalLines = result.horizontalLines;
        verticalLines = result.verticalLines;

		System.out.println(result);
        resultsArea.setText(result.toString());
	}

    @FXML
    void handleSaveFingerprint() throws FileNotFoundException, UnsupportedEncodingException {
        //zapisanie danych do pliku

//  RGB
//	    int[] verticalData = Filters
//			    .findByClass(SearchFingerprint.class)
//			    .get()
//			    .verticalData;
//	    int[] horizontalData = Filters
//			    .findByClass(SearchFingerprint.class)
//			    .get()
//			    .horizontalData;

        handleCheckLines();

        String name = inpFingerprintName.getText();
        System.out.println(name);

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("data.txt", true)));
            //PrintWriter writer = new PrintWriter("data.txt", "UTF-8");
            writer.println("#" + name);

            int i = 0;
            for (Map.Entry<Integer, Integer> entry : horizontalLines.entrySet()) {
                writer.println("H" + i + " " + entry.getValue());
                System.out.println("H" + i + " " + entry.getValue());
                i++;
            }
            i = 0;
            for (Map.Entry<Integer, Integer> entry : verticalLines.entrySet()) {
                writer.println("V" + i + " " + entry.getValue());
                System.out.println("V" + i + " " + entry.getValue());
                i++;
            }
//  RGB
//            for (int i = 0; i < verticalData.length; i++) {
//                writer.println("V" + i + " " + verticalData[i]);
//                System.out.println("V" + i + " " + verticalData[i]);
//            }
//
//            for (int i = 0; i < horizontalData.length; i++) {
//                writer.println("H" + i + " " + horizontalData[i]);
//                System.out.println("H" + i + " " + horizontalData[i]);
//            }

            writer.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Zapisywanie odcisku");
            alert.setHeaderText(null);
            alert.setContentText("Zapisano odcisk w bazie danych!");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fingerprints.removeAll(fingerprints);
        readData();
    }

    @FXML
    void handleFindFigerprint(ActionEvent actionEvent) {
        handleCheckLines();

        System.out.println("Wyszukiwanie odcisku w bazie danych");
        int minErrorIndex = 0;
        double minAverage = 1000;

        //Liczenie błędu
        System.out.println("BŁAD POMIAROWY:");

        int currentFingerprint = 0;
        for (FingerprintData fingerprint : fingerprints) {
            String name = fingerprint.getName();
            int value;
            FingerprintData tmp = new FingerprintData();

            tmp.setName(name);
            System.out.println("#" + name);
            int i = 0;
            for (Map.Entry<Integer, Integer> entry : horizontalLines.entrySet()) {
                value = Math.abs(fingerprint.getHorizontalData(i) - entry.getValue());
                tmp.setHorizontalData(i, value);
                System.out.print("H" + value + "   ");
                i++;
            }
            tmp.calcutateHorizontalAverage();
            System.out.print("srednia: " + tmp.getHorizontalAverage() + "\n");

            i = 0;
            for (Map.Entry<Integer, Integer> entry : verticalLines.entrySet()) {
                value = Math.abs(fingerprint.getVerticalData(i) - entry.getValue());
                tmp.setVerticalData(i, value);
                System.out.print("V" + value + "   ");
                i++;
            }
            tmp.calcutateVerticalAverage();
            System.out.print("srednia: " + tmp.getVerticalAverage());

            tmp.calculateAverage();
            double average = tmp.getAverage();
            System.out.println("\nSrednia: " + average);

            compareData.add(tmp);

            if(average < minAverage) {
                minAverage = average;
                minErrorIndex = currentFingerprint;
            }

            currentFingerprint++;
        }

        String foundName = fingerprints.get(minErrorIndex).getName();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Wyszukiwanie odcisku w bazie danych");
        alert.setHeaderText(null);
        String msgText;
        if(minAverage == 0.0) {
            msgText = "Odcisk w 100% należy do " + foundName;
        }
        else {
            msgText = "Odcisk najbardziej pasuje do " + foundName + "\nBłąd pomiarowy wynosi: " + minAverage;
        }
        alert.setContentText(msgText);
        alert.showAndWait();
    }


	@FXML
	void handleFileOpen(ActionEvent event) {
		getFile().ifPresent(choosen -> tryOpenFile(choosen));
	}

	@FXML
	void handleOpenTestImage() {
		File source = new File("./filter-test.jpg");
		tryOpenFile(source);
	}

	private void tryOpenFile(File source) {
		try {
			source = source.getCanonicalFile();
			openFile(source);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void openFile(File source) {
		String uri = source.toURI().toString();
		System.out.println("opening: " + uri);
		imgLeft.setImage(new Image(uri));
		enableButtons(true);
	}

    @FXML
    void handleFileSave(ActionEvent event) {
        getFile().ifPresent(file -> {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(imgLeft.getImage(), null), "png", file);
            } catch (Exception s) {
                s.printStackTrace();
            }
        });
    }

    private Optional<File> getFile() {
        FileChooser fileChooser = fileChooserSupplier.get();
        File choosen = fileChooser.showOpenDialog(root.getScene().getWindow());
        return Optional.ofNullable(choosen);
    }

	@FXML
	private void enableButtons(boolean enable) {
		boolean disable = !enable;
		btnApplyFilter.setDisable(disable);
		filterChooser.setDisable(disable);
		btnAcceptFilter.setDisable(disable);
		btnCheckLines.setDisable(disable);
        btnSaveFingerprint.setDisable(disable);
        inpFingerprintName.setDisable(disable);
        resultsArea.setDisable(disable);
        resultsLabel.setDisable(disable);
        btnFind.setDisable(disable);
    }

	@FXML
	void handleFileExit(ActionEvent event) {
		Platform.exit();
	}
}
