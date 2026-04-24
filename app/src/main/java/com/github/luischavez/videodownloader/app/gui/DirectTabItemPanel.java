package com.github.luischavez.videodownloader.app.gui;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Luis
 */
public class DirectTabItemPanel extends JPanel {

	public interface DirectTabListener {

		void onAllFinished(DirectTabItemPanel item, Integer nextNumber);

		void onAllRemoved(DirectTabItemPanel item);
	}

	private DirectTabListener listener;

	public DirectTabItemPanel(Integer nextNumber) {
		initComponents();
		initialize();

		if (nextNumber != null) {
			fromHourTextField.setText(nextNumber.toString());
		}
	}

	public long startAt() {
		long millis = 0;

		millis += Integer.valueOf(fromSecondTextField.getText().isEmpty() ? "0" : fromSecondTextField.getText()) * 1_000;
		millis += Integer.valueOf(fromMinuteTextField.getText().isEmpty() ? "0" : fromMinuteTextField.getText()) * 60 * 1_000;
		millis += Integer.valueOf(fromHourTextField.getText().isEmpty() ? "0" : fromHourTextField.getText()) * 60 * 60 * 1_000;

		return millis;
	}

	public long stopAt() {
		long millis = 0;

		millis += Integer.valueOf(toSecondTextField.getText().isEmpty() ? "0" : toSecondTextField.getText()) * 1_000;
		millis += Integer.valueOf(toMinuteTextField.getText().isEmpty() ? "0" : toMinuteTextField.getText()) * 60 * 1_000;
		millis += Integer.valueOf(toHourTextField.getText().isEmpty() ? "0" : toHourTextField.getText()) * 60 * 60 * 1_000;

		return millis;
	}

	public void setListener(DirectTabListener listener) {
		this.listener = listener;
	}

	public void removeListener() {
		listener = null;
	}

	private boolean isCompleted(JTextField textField) {
		return !textField.getText().isEmpty() && textField.getText().length() == 2;
	}

	private JTextField getPreviousFocus() {
		if (!isCompleted(fromMinuteTextField)) {
			return fromHourTextField;
		}

		if (!isCompleted(fromSecondTextField)) {
			return fromMinuteTextField;
		}

		if (!isCompleted(toHourTextField)) {
			return fromSecondTextField;
		}

		if (!isCompleted(toMinuteTextField)) {
			return toHourTextField;
		}

		if (!isCompleted(toSecondTextField)) {
			return toMinuteTextField;
		}

		return fromHourTextField;
	}

	private JTextField getNextFocus() {
		if (!isCompleted(fromHourTextField)) {
			return fromHourTextField;
		}

		if (!isCompleted(fromMinuteTextField)) {
			return fromMinuteTextField;
		}

		if (!isCompleted(fromSecondTextField)) {
			return fromSecondTextField;
		}

		if (!isCompleted(toHourTextField)) {
			return toHourTextField;
		}

		if (!isCompleted(toMinuteTextField)) {
			return toMinuteTextField;
		}

		return toSecondTextField;
	}

	private boolean resolveFocus(Object currentFocus) {
		final String name = currentFocus == null ? null : ((JTextField) currentFocus).getName();

		if (name != null && !name.isEmpty()) return true;

		JTextField nextFocusField = getNextFocus();

		if (nextFocusField != null) {
			nextFocusField.requestFocus();
			nextFocusField.grabFocus();

			return currentFocus == nextFocusField;
		}

		return true;
	}

	public void setInputFocus() {
		resolveFocus(null);
	}

	public boolean isAllCompleted() {
		return toSecondTextField.getText().length() == 2;
	}

	private void configureTextField(JTextField textField) {
		textField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					textField.setName("disable_autofocus");
				}
			}
		});

		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (listener != null && isAllCompleted()) {
					listener.onAllFinished(DirectTabItemPanel.this, null);
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (listener != null && fromHourTextField.getText().isEmpty()) {
					listener.onAllRemoved(DirectTabItemPanel.this);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		});

		textField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT
					|| e.getKeyCode() == KeyEvent.VK_RIGHT) {
					return;
				}

				Integer nextNumber = null;

				if (!Character.isDigit(e.getKeyChar())) {
					e.consume();
				} else {
					nextNumber = Integer.valueOf(String.valueOf(e.getKeyChar()));
				}

				if (!resolveFocus(e.getSource())) {
					e.consume();

					JTextField nextFocus = getNextFocus();

					if (nextFocus != null) {
						nextFocus.dispatchEvent(new KeyEvent(nextFocus, e.getID(), e.getWhen(), e.getModifiersEx(), e.getKeyCode(), e.getKeyChar(), e.getKeyLocation()));
					}
				}

				if (((JTextField) e.getSource()).getText().length() == 2) {
					e.consume();
				}

				if (listener != null && isAllCompleted()) {
					listener.onAllFinished(DirectTabItemPanel.this, nextNumber);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					Object source = e.getSource();
					int caretPosition = ((JTextField) source).getCaretPosition();
					boolean caretAtStart = caretPosition == 0;

					JTextField newTarget = (JTextField) source;

					if (caretAtStart) {
						if (source == toSecondTextField) {
							newTarget = toMinuteTextField;
						} else if (source == toMinuteTextField) {
							newTarget = toHourTextField;
						} else if (source == toHourTextField) {
							newTarget = fromSecondTextField;
						} else if (source == fromSecondTextField) {
							newTarget = fromMinuteTextField;
						} else if (source == fromMinuteTextField) {
							newTarget = fromHourTextField;
						}
					}

					if (newTarget != null && newTarget != source) {
						e.consume();

						newTarget.requestFocus();
						newTarget.grabFocus();
						newTarget.setCaretPosition(newTarget.getText().length());
					}
				}

				if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					Object source = e.getSource();
					int caretPosition = ((JTextField) source).getCaretPosition();
					boolean caretAtEnd = caretPosition == 2;

					JTextField newTarget = (JTextField) source;

					if (caretAtEnd) {
						if (source == fromHourTextField) {
							newTarget = fromMinuteTextField;
						} else if (source == fromMinuteTextField) {
							newTarget = fromSecondTextField;
						} else if (source == fromSecondTextField) {
							newTarget = toHourTextField;
						} else if (source == toHourTextField) {
							newTarget = toMinuteTextField;
						} else if (source == toMinuteTextField) {
							newTarget = toSecondTextField;
						}
					}

					if (newTarget != null && newTarget != source) {
						e.consume();

						newTarget.requestFocus();
						newTarget.grabFocus();
						newTarget.setCaretPosition(0);
					}
				}

				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					JTextField currentFocus = (JTextField) e.getSource();
					JTextField nextFocus = getNextFocus();
					JTextField previousFocus = getPreviousFocus();

					JTextField targetFocus = null;

					if (nextFocus == null) {
						targetFocus = toSecondTextField;
					} else {
						if (!nextFocus.getText().isEmpty()) {
							targetFocus = nextFocus;
						} else if (!previousFocus.getText().isEmpty()) {
							targetFocus = previousFocus;
						}
					}

					if ((currentFocus == null || (currentFocus.getName() == null || currentFocus.getName().isEmpty()))
							&& targetFocus != null && currentFocus != targetFocus) {
						e.consume();

						targetFocus.requestFocus();
						targetFocus.grabFocus();
						targetFocus.setCaretPosition(targetFocus.getText().length());

						targetFocus.dispatchEvent(new KeyEvent(targetFocus, e.getID(), e.getWhen(), e.getModifiersEx(), e.getKeyCode(), e.getKeyChar(), e.getKeyLocation()));
					}

					if (listener != null && fromHourTextField.getText().isEmpty()) {
						listener.onAllRemoved(DirectTabItemPanel.this);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_LEFT
					|| e.getKeyCode() == KeyEvent.VK_RIGHT) {
					return;
				}

				resolveFocus(e.getSource());

				if (((JTextField) e.getSource()).getText().length() == 2) {
					((JTextField) e.getSource()).setName("");
				}
			}
		});
	}

	private void initialize() {
		configureTextField(fromHourTextField);
		configureTextField(fromMinuteTextField);
		configureTextField(fromSecondTextField);
		configureTextField(toHourTextField);
		configureTextField(toMinuteTextField);
		configureTextField(toSecondTextField);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		indexLabel = new JLabel();
		hSpacer1 = new JPanel(null);
		label5 = new JLabel();
		fromHourTextField = new JTextField();
		label2 = new JLabel();
		fromMinuteTextField = new JTextField();
		label3 = new JLabel();
		fromSecondTextField = new JTextField();
		hSpacer2 = new JPanel(null);
		label4 = new JLabel();
		toHourTextField = new JTextField();
		label6 = new JLabel();
		toMinuteTextField = new JTextField();
		label7 = new JLabel();
		toSecondTextField = new JTextField();

		//======== this ========
		setLayout(new FormLayout(
			"10dlu, 2*($lcgap, default), $lcgap, 15dlu, $lcgap, 2dlu, $lcgap, 15dlu, $lcgap, 2dlu, $lcgap, 15dlu, 2*($lcgap, default), $lcgap, 15dlu, $lcgap, 2dlu, $lcgap, 15dlu, $lcgap, 2dlu, $lcgap, 15dlu",
			"default"));

		//---- indexLabel ----
		indexLabel.setText("#");
		add(indexLabel, CC.xy(1, 1));
		add(hSpacer1, CC.xy(3, 1));

		//---- label5 ----
		label5.setText("From");
		add(label5, CC.xy(5, 1));
		add(fromHourTextField, CC.xy(7, 1));

		//---- label2 ----
		label2.setText(":");
		add(label2, CC.xy(9, 1, CC.CENTER, CC.CENTER));
		add(fromMinuteTextField, CC.xy(11, 1));

		//---- label3 ----
		label3.setText(":");
		add(label3, CC.xy(13, 1, CC.CENTER, CC.CENTER));
		add(fromSecondTextField, CC.xy(15, 1));
		add(hSpacer2, CC.xy(17, 1));

		//---- label4 ----
		label4.setText("To");
		add(label4, CC.xy(19, 1));
		add(toHourTextField, CC.xy(21, 1));

		//---- label6 ----
		label6.setText(":");
		add(label6, CC.xy(23, 1));
		add(toMinuteTextField, CC.xy(25, 1));

		//---- label7 ----
		label7.setText(":");
		add(label7, CC.xy(27, 1));
		add(toSecondTextField, CC.xy(29, 1));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	public JLabel indexLabel;
	private JPanel hSpacer1;
	private JLabel label5;
	public JTextField fromHourTextField;
	private JLabel label2;
	public JTextField fromMinuteTextField;
	private JLabel label3;
	public JTextField fromSecondTextField;
	private JPanel hSpacer2;
	private JLabel label4;
	public JTextField toHourTextField;
	private JLabel label6;
	public JTextField toMinuteTextField;
	private JLabel label7;
	public JTextField toSecondTextField;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
