package be.speech;

import javax.swing.table.AbstractTableModel;

public class CommandTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private final String[] columnNames = new String[] { "Voice Command", "Program" };
	private final Class<?>[] columnClass = new Class[] { String.class, String.class };

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClass[columnIndex];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return Main.commands.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Command row = Main.commands.get(rowIndex);
		if (0 == columnIndex) {
			return row.getName();
		} else if (1 == columnIndex) {
			return row.getProgram();
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Command row = Main.commands.get(rowIndex);
		if (0 == columnIndex) {
			row.setName((String) aValue);
		} else if (1 == columnIndex) {
			row.setProgram((String) aValue);
		}
	}
}