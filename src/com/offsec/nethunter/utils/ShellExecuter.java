package com.offsec.nethunter.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * Shell 命令执行工具类
 */
public class ShellExecuter {
    private final SimpleDateFormat timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final static String TAG = "ShellExecuter";

    public ShellExecuter() {
    }

    /**
     * 执行命令并返回输出（不打印日志到 UI）
     */
    public static String execute(String s, Collection<String> args) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            stdin.write((s + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            /* 去掉最后的换行符 */
            if (output.length() > 0) output = new StringBuilder(output.substring(0, output.length() - 1));

            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("Shell Error:", line);
            }
            br.close();

            process.waitFor();
            process.destroy();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return null;
    }

    /**
     * 执行命令并返回输出（普通方式）
     */
    public String Executor(String command) {
        StringBuilder output = new StringBuilder();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    /**
     * 执行命令并返回输出（别名方法）
     */
    public String Executer(String command) {
        return Executor(command);
    }

    /**
     * 以 root 权限运行命令（不返回输出）
     */
    public void RunAsRoot(String[] command) {
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String tmpmd : command) {
                os.writeBytes(tmpmd + '\n');
            }
            os.writeBytes("exit\n");
            os.flush();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 以 root 权限运行命令, 失败时抛出异常
     */
    public String RunAsRootWithException(String command) throws RuntimeException {
        try {
            StringBuilder output = new StringBuilder();
            String line;
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            /* 去掉最后的换行符 */
            if (output.length() > 0) output = new StringBuilder(output.substring(0, output.length() - 1));

            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("Shell Error:", line);
                throw new RuntimeException(line);
            }
            br.close();

            process.waitFor();
            process.destroy();
            return output.toString();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 以 root 权限运行命令并返回输出
     */
    public String RunAsRootOutput(String command) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            /* 去掉最后的换行符 */
            if (output.length() > 0) output = new StringBuilder(output.substring(0, output.length() - 1));
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("Shell Error:", line);
            }
            br.close();
            process.waitFor();
            process.destroy();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return output.toString();
    }

    /**
     * 以 root 权限运行命令并实时输出到 TextView
     */
    public int RunAsRootOutput(String command, final TextView viewLogger) {
        int resultCode = 0;
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                final Spannable tempText = new SpannableString(line + "\n");
                final Spannable timestamp = new SpannableString("[ " + timeStamp.format(new Date()) + " ]  ");
                timestamp.setSpan(new ForegroundColorSpan(Color.parseColor("#FFD561")), 0, timestamp.length(), 0);
                tempText.setSpan(new ForegroundColorSpan(
                        line.startsWith("[!]") ? Color.CYAN :
                                line.startsWith("[+]") ? Color.GREEN :
                                        line.startsWith("[-]") ? Color.parseColor("#D81B60") :
                                                Color.WHITE), 0, tempText.length(), 0);
                viewLogger.post(() -> {
                    viewLogger.append(timestamp);
                    viewLogger.append(tempText);
                });
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e(TAG, line);
            }
            br.close();
            process.waitFor();
            process.destroy();
            resultCode = process.exitValue();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return resultCode;
    }

    /**
     * 以 root 权限运行命令并返回退出码
     */
    public int RunAsRootReturnValue(String command) {
        int resultCode = 0;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            process.waitFor();
            process.destroy();
            resultCode = process.exitValue();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return resultCode;
    }

    /**
     * 在 chroot 环境中执行命令并返回输出
     */
    public String RunAsChrootOutput(String command) {
        StringBuilder output = new StringBuilder();
        String line;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((NhPaths.BUSYBOX + " chroot " + NhPaths.CHROOT_PATH() + " " + NhPaths.CHROOT_SUDO + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH su" + '\n').getBytes());
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
            }
            /* 去掉最后的换行符 */
            if (output.length() > 0) output = new StringBuilder(output.substring(0, output.length() - 1));
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("Shell Error:", line);
            }
            br.close();
            process.waitFor();
            process.destroy();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return output.toString();
    }

    /**
     * 在 chroot 环境中执行命令并返回退出码
     */
    public int RunAsChrootReturnValue(String command) {
        int resultCode = 0;
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            stdin.write((NhPaths.BUSYBOX + " chroot " + NhPaths.CHROOT_PATH() + " " + NhPaths.CHROOT_SUDO + " -E PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH su" + '\n').getBytes());
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            process.waitFor();
            process.destroy();
            resultCode = process.exitValue();
        } catch (IOException e) {
            Log.d(TAG, "捕获到 IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Log.d(TAG, "捕获到 InterruptedException: " + ex.getMessage());
        }
        return resultCode;
    }

    /**
     * 异步读取文件内容并填充到 EditText
     */
    public void ReadFile_ASYNC(String _path, final EditText v) {
        final String command = "cat " + _path;
        new Thread(() -> {
            StringBuilder output = new StringBuilder();
            try {
                Process p = Runtime.getRuntime().exec("su -mm -c " + command);
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final String _output = output.toString();
            v.post(() -> v.setText(_output));
        }).start();
    }

    /**
     * 同步读取文件内容（请在后台线程中调用）
     */
    public String ReadFile_SYNC(String _path) {
        StringBuilder output = new StringBuilder();
        String command = "cat " + _path;
        Process p;
        try {
            p = Runtime.getRuntime().exec("su -mm -c " + command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    /**
     * 保存字符串到指定文件
     * @param contents 要写入的内容
     * @param _path    文件完整路径
     * @return 是否成功
     */
    public boolean SaveFileContents(String contents, String _path) {
        String _newCmd = "cat << 'EOF' > "+_path+"\n"+contents+"\nEOF";
        String _res = RunAsRootOutput(_newCmd);
        if (_res.isEmpty()){ // 无错误即成功
            return true;
        } else {
            Log.d("保存文件错误: ", "错误: " + _res);
            return false;
        }
    }

    /**
     * 读取文件内容
     */
    public String ReadFile(String duckyOutputFile) {
        return ReadFile_SYNC(duckyOutputFile);
    }

    /**
     * 以 root 权限运行命令并返回输出
     */
    public String RunAsRootReturnOutput(String s) {
        return RunAsRootOutput(s);
    }
}