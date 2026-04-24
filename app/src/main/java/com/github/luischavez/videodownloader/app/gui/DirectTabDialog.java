package com.github.luischavez.videodownloader.app.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.github.luischavez.videodownloader.app.task.ClipTask;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

/**
 * @author Luis
 */
public class DirectTabDialog extends JDialog implements DirectTabItemPanel.DirectTabListener, ActionListener {

	public List<DirectTabItemPanel> items;

	public DirectTabDialog(Window owner) {
		super(owner);

		initComponents();

		initialize();
	}

	private void initialize() {
		setTitle("PRESS ENTER TO CUT OR PRESS N TO QUEUE");

		items = new ArrayList<>();

		getRootPane().setOpaque(true);

		loadExcelButton.addActionListener(this);
	}

	@Override
	public void onAllFinished(DirectTabItemPanel item, Integer nextNumber) {
		Integer index = Integer.valueOf(item.indexLabel.getText());

		if (index < items.size()) return;

		addItem(nextNumber);
	}

	@Override
	public void onAllRemoved(DirectTabItemPanel item) {
		Integer index = Integer.valueOf(item.indexLabel.getText());

		if (index == 1) return;

		removeItem(index);
	}

	private DirectTabItemPanel createItem(Integer nextNumber) {
		DirectTabItemPanel item = new DirectTabItemPanel(nextNumber);

		items.add(item);

		int index = items.size();

		item.indexLabel.setText(index + "");

		return item;
	}

	private void scrollToBottom() {
		JScrollBar verticalBar = scrollPanel.getVerticalScrollBar();
		AdjustmentListener downScroller = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Adjustable adjustable = e.getAdjustable();
				adjustable.setValue(adjustable.getMaximum());
				verticalBar.removeAdjustmentListener(this);
			}
		};
		verticalBar.addAdjustmentListener(downScroller);
	}

	private void setItemFocus(int index) {
		DirectTabItemPanel selectedItem = null;

		for (DirectTabItemPanel item : items) {
			if (item.indexLabel.getText().equals(index + "")) {
				selectedItem = item;
				break;
			}
		}

		if (selectedItem != null) {
			selectedItem.requestFocus();
			selectedItem.grabFocus();

			selectedItem.setInputFocus();
		}
	}

	private void addItem(DirectTabItemPanel item) {
		itemPanel.add(item);

		setItemFocus(Integer.valueOf(item.indexLabel.getText()));

		itemPanel.revalidate();

		scrollToBottom();

		item.setListener(this);
	}

	public void addItem(Integer nextNumber) {
		final DirectTabItemPanel item = createItem(nextNumber);

		SwingUtilities.invokeLater(() -> {
			addItem(item);
		});
	}

	public DirectTabItemPanel createItem(long startSeconds, long startMinutes, long startHours,
										 long stopSeconds, long stopMinutes, long stopHours) {
		final DirectTabItemPanel item = createItem(null);

		item.fromHourTextField.setText(startHours < 10 ? "0" + startHours : startHours + "");
		item.fromMinuteTextField.setText(startMinutes < 10 ? "0" + startMinutes : startMinutes + "");
		item.fromSecondTextField.setText(startSeconds < 10 ? "0" + startSeconds : startSeconds + "");
		item.toHourTextField.setText(stopHours < 10 ? "0" + stopHours : stopHours + "");
		item.toMinuteTextField.setText(stopMinutes < 10 ? "0" + stopMinutes : stopMinutes + "");
		item.toSecondTextField.setText(stopSeconds < 10 ? "0" + stopSeconds : stopSeconds + "");

		return item;
	}

	public DirectTabItemPanel createItem(long startAt, long stopAt) {
		long startSeconds = startAt / 1_000;
		long startMinutes = startSeconds / 60;
		startSeconds %= 60;
		long startHours = startMinutes / 60;
		startMinutes %= 60;

		long stopSeconds = stopAt / 1_000;
		long stopMinutes = stopSeconds / 60;
		stopSeconds %= 60;
		long stopHours = stopMinutes / 60;
		stopMinutes %= 60;

		return createItem(startSeconds, startMinutes, startHours, stopSeconds, stopMinutes, stopHours);
	}

	public void removeItem(int index) {
		SwingUtilities.invokeLater(() -> {
			DirectTabItemPanel selectedItem = null;

			for (DirectTabItemPanel item : items) {
				if (item.indexLabel.getText().equals(index + "")) {
					selectedItem = item;
					break;
				}
			}

			if (selectedItem != null) {
				items.remove(selectedItem);
				selectedItem.removeListener();
				itemPanel.remove(selectedItem);
			}

			itemPanel.revalidate();

			setItemFocus(index - 1);
		});
	}

	public void clearAll() {
		final ArrayList<DirectTabItemPanel> oldItems = new ArrayList<>(items);

		items.clear();

		SwingUtilities.invokeLater(() -> {
			for (DirectTabItemPanel item : oldItems) {
				item.removeListener();
				itemPanel.remove(item);
			}

			itemPanel.revalidate();
		});
	}

	public void clearAll(List<ClipTask> tasks, File file) {
		final ArrayList<DirectTabItemPanel> oldItems = new ArrayList<>(items);
		final ArrayList<DirectTabItemPanel> newItems = new ArrayList<>();

		items.clear();

		for (ClipTask task : tasks) {
			if (task.getFile() == file) {
				DirectTabItemPanel newItem = createItem(task.getStartAt(), task.getStopAt());
				newItems.add(newItem);
			}
		}

		if (newItems.isEmpty()) {
			newItems.add(createItem(null));
		}

		SwingUtilities.invokeLater(() -> {
			for (DirectTabItemPanel item : oldItems) {
				item.removeListener();
				itemPanel.remove(item);
			}

			for (DirectTabItemPanel item : newItems) {
				addItem(item);
			}

			itemPanel.revalidate();
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select an excel file...");
		fileChooser.setFileFilter(new FileNameExtensionFilter("excel", "xlsx"));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setMultiSelectionEnabled(false);

		int option = fileChooser.showOpenDialog(this);

		if (option == JFileChooser.APPROVE_OPTION) {
			clearAll();

			final File selectedFile = fileChooser.getSelectedFile();

			try (FileInputStream is = new FileInputStream(selectedFile);
				 ReadableWorkbook wb = new ReadableWorkbook(is)) {
				Sheet sheet = wb.getFirstSheet();
				try (Stream<Row> rows = sheet.openStream()) {
					rows.forEach(r -> {
						try {
							SwingUtilities.invokeLater(() -> {
								if (r == null
										|| r.getCell(0) == null
										|| r.getCell(0).getRawValue() == null
										|| r.getCell(0).getRawValue().isEmpty()) return;

								long startHours = r.getCellAsNumber(0).orElse(BigDecimal.ZERO).longValue();
								long startMinutes = r.getCellAsNumber(1).orElse(BigDecimal.ZERO).longValue();
								long startSeconds = r.getCellAsNumber(2).orElse(BigDecimal.ZERO).longValue();
								long stopHours = r.getCellAsNumber(3).orElse(BigDecimal.ZERO).longValue();
								long stopMinutes = r.getCellAsNumber(4).orElse(BigDecimal.ZERO).longValue();
								long stopSeconds = r.getCellAsNumber(5).orElse(BigDecimal.ZERO).longValue();

								DirectTabItemPanel item = createItem(startSeconds, startMinutes, startHours, stopSeconds, stopMinutes, stopHours);
								addItem(item);

								itemPanel.revalidate();
							});
						} catch (Exception exception) {
							exception.printStackTrace();
						}
					});
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		videoLabel = new JLabel();
		loadExcelButton = new JButton();
		scrollPanel = new JScrollPane();
		itemPanel = new JPanel();

		//======== this ========
		setResizable(false);
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"default:grow",
			"2*(default, $lgap), fill:default:grow"));
		contentPane.add(videoLabel, CC.xy(1, 1, CC.CENTER, CC.DEFAULT));

		//---- loadExcelButton ----
		loadExcelButton.setText("Load Excel");
		contentPane.add(loadExcelButton, CC.xy(1, 3));

		//======== scrollPanel ========
		{

			//======== itemPanel ========
			{
				itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
			}
			scrollPanel.setViewportView(itemPanel);
		}
		contentPane.add(scrollPanel, CC.xy(1, 5, CC.FILL, CC.FILL));
		setSize(380, 470);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	public JLabel videoLabel;
	public JButton loadExcelButton;
	public JScrollPane scrollPanel;
	public JPanel itemPanel;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
