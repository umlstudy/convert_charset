package com.krx.chcp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Chcp {
	
	public void open() {
		Display display = new Display();
		final Shell shell = new Shell();
		shell.setSize(600, 400);
		shell.setLayout(new FillLayout());

		Composite composite = new Composite(shell, SWT.NONE);
		createUI(composite);
		shell.setText("KSC5601 -> UTF-8 변환기");
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
	
	public static void main(String[] args) {
		new Chcp().open();
	}

	private void createUI(final Composite parent) {
		GridLayoutFactory gradLayoutFactory = GridLayoutFactory.swtDefaults();
		parent.setLayout(gradLayoutFactory.create());
		GridDataFactory gridDataFactory = GridDataFactory.fillDefaults();

		Composite row = new Composite(parent, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.center = true;
		row.setLayout(rowLayout);
		Label label = new Label(row, SWT.NONE);
		label.setText("대상경로:");
		final Text targetPath = new Text(parent, SWT.BORDER);
		targetPath.setLayoutData(gridDataFactory.grab(true, false).create());
		Button changeDirectory = new Button(row, SWT.NONE);
		changeDirectory.setText("...");
		changeDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dlg = new DirectoryDialog(targetPath.getShell());
				String path = dlg.open();
				if ( path != null ) {
					targetPath.setText(path);
				}
			}
		});
		
		row = new Composite(parent, SWT.NONE);
		rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.center = true;
		row.setLayout(rowLayout);
		label = new Label(row, SWT.NONE);
		label.setText("저장경로:");
		final Text savePath = new Text(parent, SWT.BORDER);
		savePath.setLayoutData(gridDataFactory.grab(true, false).create());
		changeDirectory = new Button(row, SWT.NONE);
		changeDirectory.setText("...");
		changeDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dlg = new DirectoryDialog(savePath.getShell());
				String path = dlg.open();
				if ( path != null ) {
					savePath.setText(path);
				}
			}
		});
		
		Button convert = new Button(parent, SWT.PUSH);
	    convert.setText("Run Convert");
	    
	    
	    final StyledText console = new StyledText(parent, SWT.NONE | SWT.V_SCROLL);
	    console.setLayoutData(gridDataFactory.grab(true, true).create());
	    
	    convert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final File targetPathFile = new File(targetPath.getText());
				if ( !targetPathFile.isDirectory() ) {
					String msg = String.format("대상경로\'%s\'는 유효한 디렉토리이어야 합니다.", targetPath.getText());
					error(console, msg);
					return;
				}
				
				final File savePathFile = new File(savePath.getText());
				if ( !savePathFile.isDirectory() ) {
					String msg = String.format("저장경로\'%s\'는 유효한 디렉토리이어야 합니다.", savePath.getText());
					error(console, msg);
					return;
				}
				
				if ( savePathFile.getAbsoluteFile().equals(targetPathFile.getAbsoluteFile())) {
					String msg = String.format("저장경로와 대상경로가 같습니다. 이럴 경우 같은 파일에 오버라이트하게 됩니다.");
					error(console, msg);
					
					boolean confirm = MessageDialog.openConfirm(parent.getShell(), "확인", msg + "\n진행하시겠습니까?" );
					if ( !confirm ) {
						return;
					}
				}

				try {
					// 1.
					final List<File> targetFiles = new ArrayList<File>();
					runOnProgressMonitorDialog(parent.getShell(), false, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.setTaskName("대상 파일을 검색합니다.");
							monitor.beginTask("작업이 완료될 때까지 기다리십시오.", 1);
							monitor.subTask("대상 폴더에 파일이 많을 경우 시간이 오래 걸릴 수 있습니다.");
							
							Collection<File> files = FileUtils.listFiles(targetPathFile, new String[] {"txt"}, true);
							targetFiles.addAll(files);
							monitor.done();
						}
					});

					error(console, String.format("총 %d개의 파일을 변환합니다.", targetFiles.size()));
					
					// 2. 
					runOnProgressMonitorDialog(parent.getShell(), true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.setTaskName("대상 파일을 변경합니다.");
							monitor.beginTask("대상파일을 변경 중 ...", targetFiles.size());
							int idx = 1;
							for ( File file : targetFiles ) {
								if ( monitor.isCanceled() ) {
									error(console, "취소되었습니다.");
									return;
								};
								String outFileName = savePathFile.getAbsoluteFile() + file.getAbsolutePath().replace(targetPathFile.getAbsolutePath(), "");
								File outFile = new File(outFileName);

								String msg = String.format("%d/%d \n %s 을\n %s 으로 변환 복사 중...", idx++, targetFiles.size(), file.getPath(), outFile.getPath());
								monitor.subTask(msg);
								error(console, msg);
								
								try {
									String content = readFile(file, "KSC5601");
									boolean write = writeFile(outFile, content, "UTF-8");
									if ( !write ) {
										error(console, String.format("** %s 은 변환하지 않음", outFile.getPath()));
									}
								} catch ( ChcpException e ) {
									error(console, e);
								}
								Thread.sleep(100);
								monitor.worked(1);
							}
						}
					});
				} catch ( Exception ex ) {
					error(console, ex);
					error(console, "에러로 인하여 중단합니다.");
					ex.printStackTrace();
				}
				
				error(console, "변환이 완료되었습니다.");
			}
		});
	}

	private void error(StyledText console, Throwable t) {
		while ( t.getCause() != null ) {
			t = t.getCause();
		}
		String msg = t.getMessage();
		if ( msg == null ) {
			msg = t.getClass().getSimpleName();
		}
		
		error(console, msg);
	}
	
	private void error(final StyledText console, final String msg) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				console.append(msg+"\n");
				console.setTopIndex(console.getLineCount() - 1);
			}
		});
	}
	
	private String readFile(File file, String charset) throws ChcpException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] b = new byte[(int) file.length()];
			int len = b.length;
			int total = 0;
			while (total < len) {
				int result = in.read(b, total, len - total);
				if (result == -1) {
					break;
				}
				total += result;
			}
			
			return new String(b, charset);
		} catch ( IOException e ) {
			throw new ChcpException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private boolean writeFile(final File file, String content, String charset) throws ChcpException {
		OutputStream out = null;
		try {
			if ( file.exists() ) {
				final Display display = Display.getDefault();
				final ObjectOwner confirmObject = new ObjectOwner();
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						boolean confirm = MessageDialog.openConfirm(display.getActiveShell(), 
								"확인", 
								String.format("파일이 이미 존재합니다. 덮어 쓰시겠습니까?\n(%s)", file.getPath()));
						confirmObject.setObject(confirm);
					}
				});
				
				if ( !(Boolean)confirmObject.getObject() ) {
					return false;
				}
			} else {
				file.createNewFile();
			}
			
			out = new FileOutputStream(file);
			out = new BufferedOutputStream(out);
			byte[] bytes = content.getBytes(charset);
			out.write(bytes);
			out.flush();
		} catch ( IOException e ) {
			throw new ChcpException(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
		
		return true;
	}

	private void runOnProgressMonitorDialog(Shell shell, boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws Exception {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell) {
			public void configureShell(Shell shell) {
				shell.setText("진행중...");
			}
		};
		pmd.run(fork, cancelable, runnable);
	}
}
