package com.yoursway.crashkit.demo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.yoursway.crashkit.CrashKit;
import com.yoursway.crashkit.exceptions.Failure;

public class Main {

    public static final CrashKit crashKit = CrashKit.connectApplication(
	    "FuckUpper", "0.99a.N20090216", "ys", "fuckupper",
	    new String[] {"com.yoursway.crashkit.demo"});

    static class HellsBrokeLoose extends Failure {
	private static final long serialVersionUID = 1L;
    }

    static class UnhandledEventLoopException extends Failure {
	private static final long serialVersionUID = 1L;

	public UnhandledEventLoopException(Throwable cause) {
	    super(cause);
	}
    }

    public static void main(String[] args) {
	Display display = new Display();
	Shell shell = new Shell(display);
	shell.setLayout(new GridLayout(1, false));

	Button button = new Button(shell, SWT.PUSH);
	button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
		false));
	button.setText("Fuck up");
	button.addSelectionListener(new SelectionAdapter() {
	    public void widgetSelected(SelectionEvent e) {
		throw new IllegalArgumentException("Fuck.");
	    }
	});

	final Button button2 = new Button(shell, SWT.PUSH);
	button2.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
		false));
	button2.setText("Go nuts");
	button2.addSelectionListener(new SelectionAdapter() {
	    public void widgetSelected(SelectionEvent e) {
		button2.setText(null);
	    }
	});

	final Button button3 = new Button(shell, SWT.PUSH);
	button3.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
		false));
	button3.setText("Break all hells loose");
	button3.addSelectionListener(new SelectionAdapter() {
	    public void widgetSelected(SelectionEvent e) {
		new Thread() {
		    public void run() {
			throw new NullPointerException("Mua-ha-ha");
		    }
		}.start();
	    }
	});

	final Button button4 = new Button(shell, SWT.PUSH);
	button4.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
		false));
	button4.setText("Heyloo");
	button4.addSelectionListener(new SelectionAdapter() {
	    public void widgetSelected(SelectionEvent e) {
		CrashKit.major(new IllegalArgumentException());
	    }
	});

	shell.pack();
	shell.open();
	while (!shell.isDisposed()) {
	    try {
		if (!display.readAndDispatch())
		    display.sleep();
	    } catch (Throwable e) {
		double random = Math.random();
		String foo = (random > 0.66 ? "A very very very very very long data"
			: (random > 0.33 ? "2" : "1"));
		CrashKit.bug(new UnhandledEventLoopException(e).add("foo", foo)
			.add("another_detail_2", e.hashCode() % 2).add(
				"another_detail_3", e.hashCode() % 3).add(
				"another_detail_5", e.hashCode() % 5).add(
				"another_detail_7", e.hashCode() % 7).add(
				"another_detail_11", e.hashCode() % 11).add(
				"another_detail_13", e.hashCode() % 13).add(
				"some_additional_data", "42").add(
				"more_additional_data", "42").add(
				"even_more_additional_data", "42"));
	    }
	}
	display.dispose();
    }
}
