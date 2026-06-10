package com.securegate.stego;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DwtStegoEngine {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    /**
     * Performs a 1-level 2D Haar Discrete Wavelet Transform.
     * Returns a Mat containing [LL, HL; LH, HH]
     */
    private Mat haarDwt2D(Mat image) {
        int rows = image.rows();
        int cols = image.cols();

        // Ensure even dimensions
        if (rows % 2 != 0) rows--;
        if (cols % 2 != 0) cols--;

        Mat input = new Mat(image, new Rect(0, 0, cols, rows));
        Mat output = new Mat(rows, cols, CvType.CV_32F);

        int halfRows = rows / 2;
        int halfCols = cols / 2;

        for (int i = 0; i < halfRows; i++) {
            for (int j = 0; j < halfCols; j++) {
                double a = input.get(2 * i, 2 * j)[0];
                double b = input.get(2 * i, 2 * j + 1)[0];
                double c = input.get(2 * i + 1, 2 * j)[0];
                double d = input.get(2 * i + 1, 2 * j + 1)[0];

                double ll = (a + b + c + d) / 2.0;
                double hl = (a - b + c - d) / 2.0;
                double lh = (a + b - c - d) / 2.0;
                double hh = (a - b - c + d) / 2.0;

                output.put(i, j, ll);
                output.put(i, j + halfCols, hl);
                output.put(i + halfRows, j, lh);
                output.put(i + halfRows, j + halfCols, hh);
            }
        }
        return output;
    }

    /**
     * Performs a 1-level 2D Inverse Haar Discrete Wavelet Transform.
     */
    private Mat inverseHaarDwt2D(Mat dwtImage) {
        int rows = dwtImage.rows();
        int cols = dwtImage.cols();
        Mat output = new Mat(rows, cols, CvType.CV_32F);

        int halfRows = rows / 2;
        int halfCols = cols / 2;

        for (int i = 0; i < halfRows; i++) {
            for (int j = 0; j < halfCols; j++) {
                double ll = dwtImage.get(i, j)[0];
                double hl = dwtImage.get(i, j + halfCols)[0];
                double lh = dwtImage.get(i + halfRows, j)[0];
                double hh = dwtImage.get(i + halfRows, j + halfCols)[0];

                double a = (ll + hl + lh + hh) / 2.0;
                double b = (ll - hl + lh - hh) / 2.0;
                double c = (ll + hl - lh - hh) / 2.0;
                double d = (ll - hl - lh + hh) / 2.0;

                output.put(2 * i, 2 * j, a);
                output.put(2 * i, 2 * j + 1, b);
                output.put(2 * i + 1, 2 * j, c);
                output.put(2 * i + 1, 2 * j + 1, d);
            }
        }
        return output;
    }

    public Mat embed(Mat cover, byte[] payload) throws Exception {
        byte[] payloadWithLength = new byte[payload.length + 4];
        payloadWithLength[0] = (byte) (payload.length >> 24);
        payloadWithLength[1] = (byte) (payload.length >> 16);
        payloadWithLength[2] = (byte) (payload.length >> 8);
        payloadWithLength[3] = (byte) (payload.length);
        System.arraycopy(payload, 0, payloadWithLength, 4, payload.length);

        boolean[] bits = new boolean[payloadWithLength.length * 8];
        for (int i = 0; i < payloadWithLength.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = ((payloadWithLength[i] >> (7 - j)) & 1) == 1;
            }
        }

        Mat ycrcb = new Mat();
        Imgproc.cvtColor(cover, ycrcb, Imgproc.COLOR_BGR2YCrCb);
        List<Mat> channels = new ArrayList<>();
        Core.split(ycrcb, channels);

        Mat yChannel = channels.get(0);
        Mat yFloat = new Mat();
        yChannel.convertTo(yFloat, CvType.CV_32F);

        // Perform DWT
        Mat dwtImage = haarDwt2D(yFloat);

        int halfRows = dwtImage.rows() / 2;
        int halfCols = dwtImage.cols() / 2;

        if (bits.length > halfRows * halfCols) {
            throw new Exception("Payload too large for this cover image DWT sub-band");
        }

        // Quantization step for embedding in HH band
        double Q = 8.0;

        int bitIdx = 0;
        outer: for (int i = halfRows; i < dwtImage.rows(); i++) {
            for (int j = halfCols; j < dwtImage.cols(); j++) {
                if (bitIdx < bits.length) {
                    double coef = dwtImage.get(i, j)[0];
                    double remainder = Math.abs(coef) % Q;
                    double base = Math.abs(coef) - remainder;

                    boolean bit = bits[bitIdx];

                    double newRemainder;
                    if (bit) {
                        newRemainder = Q * 0.75;
                    } else {
                        newRemainder = Q * 0.25;
                    }

                    double newCoef = base + newRemainder;
                    if (coef < 0) newCoef = -newCoef;

                    dwtImage.put(i, j, newCoef);
                    bitIdx++;
                } else {
                    break outer;
                }
            }
        }

        // Perform IDWT
        Mat reconstructedYFloat = inverseHaarDwt2D(dwtImage);

        Mat reconstructedY = new Mat();
        reconstructedYFloat.convertTo(reconstructedY, CvType.CV_8U);

        // Ensure same size if odd dimensions were trimmed
        if (reconstructedY.rows() < yChannel.rows() || reconstructedY.cols() < yChannel.cols()) {
             Mat finalY = new Mat();
             yChannel.copyTo(finalY);
             Rect roi = new Rect(0, 0, reconstructedY.cols(), reconstructedY.rows());
             reconstructedY.copyTo(finalY.submat(roi));
             channels.set(0, finalY);
        } else {
             channels.set(0, reconstructedY);
        }

        Core.merge(channels, ycrcb);
        Mat stegoImage = new Mat();
        Imgproc.cvtColor(ycrcb, stegoImage, Imgproc.COLOR_YCrCb2BGR);

        yFloat.release();
        dwtImage.release();
        reconstructedYFloat.release();
        reconstructedY.release();
        ycrcb.release();
        for (Mat m : channels) m.release();

        return stegoImage;
    }

    public byte[] extract(Mat stego) throws Exception {
        Mat ycrcb = new Mat();
        Imgproc.cvtColor(stego, ycrcb, Imgproc.COLOR_BGR2YCrCb);
        List<Mat> channels = new ArrayList<>();
        Core.split(ycrcb, channels);

        Mat yChannel = channels.get(0);
        Mat yFloat = new Mat();
        yChannel.convertTo(yFloat, CvType.CV_32F);

        Mat dwtImage = haarDwt2D(yFloat);

        int halfRows = dwtImage.rows() / 2;
        int halfCols = dwtImage.cols() / 2;

        double Q = 8.0;

        int bitIdx = 0;
        int payloadLength = 0;

        for (int i = halfRows; i < dwtImage.rows(); i++) {
            for (int j = halfCols; j < dwtImage.cols(); j++) {
                double coef = dwtImage.get(i, j)[0];
                double remainder = Math.abs(coef) % Q;

                int bit = (remainder > Q / 2) ? 1 : 0;

                if (bitIdx < 32) {
                    payloadLength = (payloadLength << 1) | bit;
                } else {
                    break;
                }
                bitIdx++;
            }
            if (bitIdx >= 32) break;
        }

        if (payloadLength <= 0 || payloadLength > (halfRows * halfCols / 8)) {
            throw new Exception("Invalid payload length extracted: " + payloadLength);
        }

        byte[] payload = new byte[payloadLength];
        bitIdx = 0;
        int byteIdx = 0;
        int currentByte = 0;
        int skip = 32;

        for (int i = halfRows; i < dwtImage.rows(); i++) {
            for (int j = halfCols; j < dwtImage.cols(); j++) {
                if (skip > 0) {
                    skip--;
                    continue;
                }

                if (byteIdx < payloadLength) {
                    double coef = dwtImage.get(i, j)[0];
                    double remainder = Math.abs(coef) % Q;
                    int bit = (remainder > Q / 2) ? 1 : 0;

                    currentByte = (currentByte << 1) | bit;
                    bitIdx++;

                    if (bitIdx == 8) {
                        payload[byteIdx++] = (byte) currentByte;
                        bitIdx = 0;
                        currentByte = 0;
                    }
                } else {
                    break;
                }
            }
            if (byteIdx >= payloadLength) break;
        }

        yFloat.release();
        dwtImage.release();
        ycrcb.release();
        for (Mat m : channels) m.release();

        return payload;
    }
}
