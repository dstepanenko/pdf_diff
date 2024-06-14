package pdfComparison;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import name.fraser.neil.plaintext.diff_match_patch;

public class pdfComparator {

	public static String HOME = "/home/guestwnet/Downloads/";
	public static int MAX_DISTANCE = 10;
	
	public static ArrayList<String> ReadPdf(String path) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		PdfReader reader = new PdfReader(path);
        for (int i = 1; i <= reader.getNumberOfPages(); ++i) {
            SimpleTextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
            
            String text = PdfTextExtractor.getTextFromPage(reader, i, strategy);
            result.add(text);
        }
        reader.close();
        return result;
	}
	
	static void cleanDirectory(String folderPath) {
		File folder = new File(folderPath);
		for (File file: folder.listFiles()) {
		    if (file.getName().endsWith(".jpg")) {
		        file.delete(); 
		    }
		}
	}
	public static TreeMap <String, ImmutablePair <Integer, Integer>> saveImages(String path, String outputDir) {		
		TreeMap <String, ImmutablePair <Integer, Integer>> pictureSize = new TreeMap <String, ImmutablePair <Integer, Integer>>();
		PDDocument pdDoc = null;
	    try {
			File file = new File(path);
		    pdDoc = Loader.loadPDF(file);
		    PDPageTree pages = pdDoc.getDocumentCatalog().getPages();
		    int pageNum = 0;
		    for (PDPage page : pages) {
		    	++pageNum;
                PDResources pdResources = page.getResources();
                int i = 1;
                for (COSName name : pdResources.getXObjectNames()) {
                    PDXObject o = pdResources.getXObject(name);
                    if (o instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject)o;
                        String filename = outputDir + "extracted-image-page" + pageNum + "-index-" + i + ".jpg";
                        ImageIO.write(image.getImage(), "jpg", new File(filename));
                        int height = image.getHeight();
                        int width = image.getWidth();
                        pictureSize.put(filename, new ImmutablePair <>(height, width));
                        ++i;
                    }
                }
                break;
		    }
		    pdDoc.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	        try {
	            if (pdDoc != null)
	                pdDoc.close();
	        } catch (Exception e1) {
	            e1.printStackTrace();
	        }
	    }

	    return pictureSize;

	}

	public static BufferedImage getImageFromPdf(String pdfPath) {		
		PDDocument pdDoc = null;
		BufferedImage result = null;
	    try {
			File file = new File(pdfPath);
		    pdDoc = Loader.loadPDF(file);
		    PDPageTree pages = pdDoc.getDocumentCatalog().getPages();
		    for (PDPage page : pages) {
                PDResources pdResources = page.getResources();
                for (COSName name : pdResources.getXObjectNames()) {
                    PDXObject o = pdResources.getXObject(name);
                    if (o instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject)o;
                        if (result != null) {
                        	System.out.println("Warning: getImageFromPdf " + pdfPath + " file contains more than 1 image inside");
                        	return result;
                        }
                        result = image.getImage();
                        
                    }
                }
                break;
		    }
		    pdDoc.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	        try {
	            if (pdDoc != null)
	                pdDoc.close();
	        } catch (Exception e1) {
	            e1.printStackTrace();
	        }
	    }

	    return result;

	}

	public static ArrayList <ImmutablePair <String, Integer>> getImageDiff(String origPicturesPath, String modifPicturesPath, int pageNum) {
		ArrayList <ImmutablePair <String, Integer>> result = new ArrayList <ImmutablePair <String, Integer>>();
		int origImageCount = 0, modifImageCount = 0;
		for (int imageNum = 1; imageNum < 100; ++imageNum) {
			String path1 = origPicturesPath + "extracted-image-page" + pageNum + "-index-" + imageNum + ".jpg";
			String path2 = modifPicturesPath + "extracted-image-page" + pageNum + "-index-" + imageNum + ".jpg";
			File file1 = new File(path1);
			File file2 = new File(path2);
			if (file1.exists()) {
				origImageCount = imageNum;
			}
			if (file2.exists()) {
				modifImageCount = imageNum;
			}
		}
		Integer changed[][] = new Integer[origImageCount + 3][modifImageCount + 3];
		Integer previous[][] = new Integer[origImageCount + 3][modifImageCount + 3];
		final int MAX_VALUE = origImageCount + modifImageCount + 5;
        Arrays.stream(changed).forEach(row -> Arrays.fill(row, MAX_VALUE));
        Arrays.stream(previous).forEach(row -> Arrays.fill(row, -1));
		previous[1][1] = -1;
		changed[1][1] = 0;

		for (int id1 = 1; id1 < origImageCount + 2; ++id1) {
			String path1 = origPicturesPath + "extracted-image-page" + pageNum + "-index-" + id1 + ".jpg";
			File file1 = new File(path1);			
			for (int id2 = 1; id2 < modifImageCount + 2; ++id2) {
				
				if (changed[id1 + 1][id2] > changed[id1][id2] + 1) {
					changed[id1 + 1][id2] = changed[id1][id2] + 1;
					previous[id1 + 1][id2] = 1;
				}
				if (changed[id1][id2 + 1] > changed[id1][id2] + 1) {
					changed[id1][id2 + 1] = changed[id1][id2] + 1;
					previous[id1][id2 + 1] = 2;
				}
				if (id1 < origImageCount + 1 && id2 < modifImageCount + 1 && changed[id1 + 1][id2 + 1] > changed[id1][id2]) {
					String path2 = modifPicturesPath + "extracted-image-page" + pageNum + "-index-" + id2 + ".jpg";
					File file2 = new File(path2);
					try {
						if (FileUtils.contentEquals(file1, file2)) {
							changed[id1 + 1][id2 + 1] = changed[id1][id2];
							previous[id1 + 1][id2 + 1] = 0;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println("Error: failed to run FileUtils.contentEquals");
						e.printStackTrace();
					}
				}
			}
		}
		for (int i = origImageCount + 1, j = modifImageCount + 1; i + j > 2; ) {
			String path1 = origPicturesPath + "extracted-image-page" + pageNum + "-index-" + (i - 1) + ".jpg";
			String path2 = modifPicturesPath + "extracted-image-page" + pageNum + "-index-" + (j - 1) + ".jpg";

			switch (previous[i][j]) {
			case (0):
				result.add(new ImmutablePair<>(path1, 0));
				--i;
				--j;
				break;
			case (1):
				result.add(new ImmutablePair<>(path1, -1));
				--i;
				break;
			case (2):
				result.add(new ImmutablePair<>(path2, 1));
				--j;
				break;
			}
		}
		Collections.reverse(result);
		return result;
	}
	// receives 2 texts as a 2 lists of paragraphs. Will compare them paragraph by paragraph
	public static void printDiff(ArrayList<String> textPages1, ArrayList<String> textPages2,
			String origPicturesPath, String modifPicturesPath,
			TreeMap <String, ImmutablePair <Integer, Integer>> imageSize,
			String pdfResultPath) throws Exception{
		if (textPages1.isEmpty()) {
			System.out.println("printDiff received empty text");
			return;
		}
		final Font RED =
			    new Font(FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.RED);
		final Font GREEN =
			    new Font(FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GREEN);
		final Font GRAY =
			    new Font(FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
		
		diff_match_patch dmp = new diff_match_patch();
		
		int pageCount = Math.max(textPages1.size(), textPages2.size());
		
	    Document document = new Document();
	    try
	    {
	    	PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfResultPath));
	    	document.open();
		    for (int pageId = 1; pageId < pageCount + 1; ++pageId) {
		    	String text1 = "", text2 = "";
		    	if (pageId < textPages1.size()) {
		    		text1 = textPages1.get(pageId - 1);
		    	}
		    	if (pageId < pageCount) {
		    		text2 = textPages2.get(pageId - 1);
		    	}
			    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(text1, text2);
		    	for (diff_match_patch.Diff diffItem: diff) {
		    		Font font;
		    		switch (diffItem.operation) {
		    		
		    		case EQUAL:
		    			font = GRAY;
		    			break;
		    		case DELETE:
		    			font = RED;
		    			break;
		    		case INSERT:
		    			font = GREEN;
	    				break;
		    		default:
	    					throw new Exception("Diff returned unknown font");	    			
		    		}
		    		String[] textItems = diffItem.text.split("\n");
		    		int linesEmpty = 0;
		    		Paragraph paragraph = new Paragraph();
		    		for (String textPart: textItems) {
		    			if (textPart.isBlank()) {
		            		linesEmpty += 1;
		            		continue;
		            	}
	
		            	if (linesEmpty > 0) {//beginning of the new paragraph
		            		if (!paragraph.isEmpty()) {
		            			document.add(paragraph);
		            		}
		            		paragraph = new Paragraph();
		            		linesEmpty = 0;
		            	}
		            	paragraph.add(new Chunk(textPart, font));
		            }
	
		            if (!paragraph.isEmpty()) {
		            	document.add(paragraph);
		            }
		        }
		    	// processing images
		    	ArrayList <ImmutablePair <String, Integer>> imageDiffs = getImageDiff(origPicturesPath, modifPicturesPath, pageId);
		    	for(ImmutablePair <String, Integer> imageDiff: imageDiffs) {
		    		BaseColor color = null;
		    		String imagePath = imageDiff.getKey();
		    		switch (imageDiff.getValue()) {
		    		case (0):
		    			color = BaseColor.GRAY;
		    			break;
		    		case (1):
		 		    	color = BaseColor.GREEN;
		    			break;
		    		case (-1):
		    			color = BaseColor.RED;
		    			break;
		    		default:
		    			System.out.println("image diff error: wrong image type: should be one of -1, 0, 1");
		    		}
		    		Image pic = Image.getInstance(imagePath);
	    			pic.scaleToFit(300, 300);
		    		pic.setBorder(Rectangle.BOX);
	    			pic.setBorderColor(color);
	    			pic.setBorderWidth(MAX_DISTANCE);
		    		document.add(pic);
		    	}
		    }

	        document.close();
	        writer.close();
	    } catch (DocumentException e)
	    {
	    	e.printStackTrace();
	    } catch (FileNotFoundException e)
	    {
	    	e.printStackTrace();
	    }
	}

	// the doc on how to use this func can be found in the main function below
	public static void calcPdfDiff(String origPdfPath, String modifPdfPath,
			String origPicturesFolderPath, String modifPicturesFolderPath, String pdfResultPath) {
		// TODO Auto-generated method stub
		
		ArrayList<String> origText = new ArrayList<String>();
		ArrayList<String> modifText = new ArrayList<String>();
		try {
			origText = ReadPdf(origPdfPath);
			modifText = ReadPdf(modifPdfPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// remove jpg files in origPicturesFolderPath
		cleanDirectory(origPicturesFolderPath);
		TreeMap <String, ImmutablePair <Integer, Integer>> imageSize = saveImages(origPdfPath, origPicturesFolderPath);
		// remove jpg files in modifPicturesFolderPath
		cleanDirectory(modifPicturesFolderPath);
		TreeMap <String, ImmutablePair <Integer, Integer>> modifSize = saveImages(modifPdfPath, modifPicturesFolderPath);
		
		imageSize.putAll(modifSize);
		try {
			printDiff(origText, modifText, origPicturesFolderPath, modifPicturesFolderPath, imageSize, pdfResultPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// anyway cleanup origPicturesFolderPath and modifPicturesFolderPath
			cleanDirectory(origPicturesFolderPath);
			cleanDirectory(modifPicturesFolderPath);
		}
	}
	
	static BufferedImage drawCircle(BufferedImage bufferedImage, int centerX, int centerY, int radius) {
		Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.green);
		g.drawOval(centerX, centerY, radius, radius);  
		g.dispose(); 
		return bufferedImage;
	}
	
    static BufferedImage getGrayImage(BufferedImage colorImage) {
        BufferedImage grayImage = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(),  
                BufferedImage.TYPE_BYTE_GRAY);  
        Graphics g = grayImage.getGraphics();  
        g.drawImage(colorImage, 0, 0, null);  
        g.dispose(); 
        return grayImage;
    }
	
    static BufferedImage getImageFromFile(String path) {
    	if (path.endsWith(".pdf")) return getImageFromPdf(path);
    	if (path.endsWith("jpg") || path.endsWith("png") || path.endsWith("jpeg")) {
    		try {
    			File file = new File(path);
    			
    			return ImageIO.read(file);
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			return null;
    		}
    	}
    	System.out.println("getImageFromFile error: unsupported input format! Supported formats are jpg, jpeg, png and pdf");
    	return null;
    }
    
	// the doc on how to use this func can be found in the main function below
	public static void calcDiffBetweenImages(String path1, String path2, String destinationPath) {
		BufferedImage image1 = getImageFromFile(path1);
		BufferedImage image2 = getImageFromFile(path2);
		
		if (image1 == null || image1 == null) {
			System.out.println("calcDiffBetweenImages error: failed to read input images");
			return;
		}
		BufferedImage image = image2;

		if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
			System.out.println("Error: Images can't be compared, since they don't have same size");
			return;
		}
		int width = image1.getWidth();
		int height = image1.getHeight();

        BufferedImage grayImage1 = getGrayImage(image1);
        BufferedImage grayImage2 = getGrayImage(image2);
        
		boolean[][] changed = new boolean[width][height];
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				changed[x][y] = grayImage1.getRGB(x, y) != grayImage2.getRGB(x, y);
			}
		}
		
		boolean[][] visited = new boolean[width][height];
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				if (visited[x][y] || !changed[x][y]) continue;
				ArrayList <ImmutablePair <Integer, Integer> > queue = new ArrayList<>(Arrays.asList(new ImmutablePair <>(x, y)));
				int head = 0;
				visited[x][y] = true;
				int minX = x, maxX = x;
				int minY = y, maxY = y;
					
				while (head < queue.size()) {
					ImmutablePair <Integer, Integer> vertex = queue.get(head);
					++head;
					int x0 = vertex.getKey();
					int y0 = vertex.getValue();
					for (int dx = -3; dx < 4; ++dx) {
						int newX = x0 + dx;
						if (newX < 0 || newX > width - 1) continue;
						for (int dy = -3; dy < 4; ++dy) {
							int newY = y0 + dy;
							if (newY < 0 || newY > height - 1) continue;
							if (!visited[newX][newY] && changed[newX][newY]) {
								queue.add(new ImmutablePair <>(newX, newY));
								visited[newX][newY] = true;
								minX = Math.min(minX, newX);
								maxX = Math.max(maxX, newX);
								minY = Math.min(minY, newY);
								maxY = Math.max(maxY, newY);
							}
						}
					}
				}
				
				if (maxX - minX < 4 || maxY - minY < 4) continue;
				int radius = Math.max(maxX - minX, maxY - minY) + 6;
				
				minX = Math.max(minX - 3, 1);
				minY = Math.max(minY - 3, 1);
				image = drawCircle(image, minX, minY, radius);
			}
		}
		try {
			ImageIO.write(image, "JPEG", new File(destinationPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		// to calculate diff between 2 pdfs (text + images that were changed inside this pdf)
		// you need to call calcPdfDiff method. The changes in text will be highlighted with
		// green for the added text and red for the removed text. Similarly, images will be framed
		// in green for the added pictures and in red for the deleted
		// parameters: origPdfPath and modifPdfPath - paths to the original and changed pdf files.
		// origPicturesFolderPath and modifPicturesFolderPath - folders that will be used for temporarily
		// storing of the images (will be cleaned out later)
		// pdfResultPath - path to the resulting pdf containing diff between pdfs.
		///*
		String origPdfPath = HOME + "OrigPic.pdf";
		String modifPdfPath = HOME + "ModifPic.pdf";
		String origPicturesFolderPath = HOME + "OrigPics/";
		String modifPicturesFolderPath = HOME + "ModifPics/";
		String pdfResultPath = HOME + "Diff.pdf";

		calcPdfDiff(origPdfPath, modifPdfPath, origPicturesFolderPath, modifPicturesFolderPath, pdfResultPath); 
		//*/
		
		// to calculate difference between 2 images you need to call calcDiffBetweenImages method
		// the method works only for images of the same formats (same width and height)
		// the method finds areas that was changed in comparison between original and modified picture
		// and mark all such areas with a green circles. areas smaller than 4x4 pixels are ignored
		// parameters: origPic and modifPic - paths to the original and modified image in jpg/png/pdf file
		// diffPic - path to the resulting jpg
		// in case input images is stored inside pdf, it should contain exacty one image
		//String origPic = "/home/guestwnet/Downloads/roomOrig6.jpg";
		//String modifPic = "/home/guestwnet/Downloads/roomModif6.jpg";
		/*
		String origPic = "/home/guestwnet/Downloads/pdfImageOrig.pdf";
		String modifPic = "/home/guestwnet/Downloads/pdfImageModif.pdf";
		String diffPic = "/home/guestwnet/Downloads/Diff.jpg";
		calcDiffBetweenImages(origPic, modifPic, diffPic);
		*/
	}
}
