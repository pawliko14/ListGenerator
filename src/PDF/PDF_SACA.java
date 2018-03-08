package PDF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import WB.DBConnection;

public class PDF_SACA {

	public static Font ffont = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 10); 
	public static Font ffont2 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 8); 
	public static Font ffont3 = FontFactory.getFont("times", BaseFont.CP1250, BaseFont.EMBEDDED, 6); 

	public static void create(){
		Connection myConn = DBConnection.dbConnector();
		Document ListMech = new Document(PageSize.A4.rotate());
		Document ListK = new Document(PageSize.A4.rotate());
		Document List = new Document(PageSize.A4.rotate());
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godz = new SimpleDateFormat("HH;mm");
		Calendar date = Calendar.getInstance();
		PdfWriter writerM;
		PdfWriter writerK;
		PdfWriter writer;
		try{
			String path = PDF.Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/";
			String nameMech = "SACA mechaniczne.pdf";
			String nameK = "SACA karoseria.pdf";
			String name = "SACA.pdf";
			File fMech = new File(path+nameMech);
			File fK = new File(path+nameK);
			File f = new File(path+name);
			if(fMech.exists() && !fMech.isDirectory())
				nameMech = godz.format(date.getTime())+" "+nameMech;
			writerM = PdfWriter.getInstance(ListMech, new FileOutputStream(path+nameMech));
			if(fK.exists() && !fK.isDirectory())
				nameK = godz.format(date.getTime())+" "+nameK;
			writerK = PdfWriter.getInstance(ListK, new FileOutputStream(path+nameK));
			if(f.exists() && !f.isDirectory())
				name = godz.format(date.getTime())+" "+name;
			writer = PdfWriter.getInstance(List, new FileOutputStream(path+name));
			ListMech.open();
			ListK.open();
			List.open();
			writerM.setPageEvent(new PDF_MyFooter());
			writerK.setPageEvent(new PDF_MyFooter());
			writer.setPageEvent(new PDF_MyFooter());
			PdfPTable tableM = new PdfPTable(12);
			PdfPTable tableK = new PdfPTable(12);
			PdfPTable table = new PdfPTable(12);
			float widths[] = new float[] {20, 20, 20, 20, 40, 40, 10, 10, 10, 10, 20, 4};
			Paragraph M = new Paragraph("Typ: S - spawane, M - mechaniczne \n", ffont2);
			Paragraph K = new Paragraph("Typ: K - karoseria \n", ffont2);
			Paragraph a = new Paragraph("Typ:  S - spawane, M - mechaniczne, K - karoseria \n", ffont2);
			ListMech.add(M);
			ListK.add(K);
			List.add(a);
			addHeader(tableM);
			addHeader(tableK);
			addHeader(table);
			Statement pobierzProjectSchedule = myConn.createStatement();
			String sql = "Select nrMaszyny, opis, klient, dataprodukcji, dataKoniecMontazu, komentarz from calendar where Zakonczone = 0 order by dataProdukcji";
			ResultSet ProjectSchedule = pobierzProjectSchedule.executeQuery(sql);
			
			while(ProjectSchedule.next()){
				
				String calyNumer = ProjectSchedule.getString("nrMaszyny");
				String ProductionDate = ProjectSchedule.getString("dataProdukcji");
				String MontageFinishDate = ProjectSchedule.getString("dataKoniecMontazu");
				String ProjectName = ProjectSchedule.getString("Opis");
				String klient = ProjectSchedule.getString("klient");
				
				System.out.println("SACA "+calyNumer);
				boolean headerM = false;
				boolean headerK = false;
				boolean header = false;
				Statement takeParts = myConn.createStatement();
				ResultSet parts = takeParts.executeQuery("SELECT ItemNo, ItemDesc, MatSource, ConsumerOrder, Quantity FROM partsoverview " +
						"where OrderNo = '"+calyNumer+"' and MatSource like '119003%' order by MatSource"); 
				String nrDostawcy = "", nrZamDost = "", zlozNadGrupa="", zlozNadNumer="";
				String PrevArticleNo = "";
				int ile = 0;
				while(parts.next()){
					String ArticleNo = parts.getString("ItemNo");
					if(ArticleNo.equals(PrevArticleNo)){
						ile++;
					}
					else
						ile = 1;
					PrevArticleNo = ArticleNo;
					String ArticleName = parts.getString("ItemDesc");
					String nrZam = parts.getString("MatSource");
					String zlozNadrzedne = parts.getString("ConsumerOrder");
					String ileDoProj = parts.getString("Quantity");
					String nrZamowienia="", zamowiono="", dostar="", jednostka="", dataDost = "", dataZam = ""; 
					for(int i = 0; i<nrZam.length(); i++){
						if(nrZam.charAt(i)=='/'){
							nrDostawcy = nrZam.substring(0, i);
							nrZamDost = nrZam.substring(i+1, nrZam.length());	
							break;
						}
					}
					for(int i = 0; i<zlozNadrzedne.length(); i++){
						if(zlozNadrzedne.charAt(i)=='/'){
							zlozNadGrupa = zlozNadrzedne.substring(0, i);
							zlozNadNumer = zlozNadrzedne.substring(i+1, zlozNadrzedne.length());	
							break;
						}
					}
					Statement wybierzZamowienie = myConn.createStatement();
					ResultSet zamowienie = wybierzZamowienie.executeQuery("SELECT besteld, geleverd, besteleenheid, besteldatum from bestellingdetail " +
							"where leverancier = '"+nrDostawcy+"' and ordernummer = '"+nrZamDost+"' and artikelcode = '"+ArticleNo+"'");
					int j = 1;
					while(zamowienie.next()){
						if(j==ile){
							Statement wybierzNrZamowienia = myConn.createStatement();
							ResultSet nrZamowienia2 = wybierzNrZamowienia.executeQuery("SELECT bestelbon, leverdatum from bestelling " +
									"where leverancier = '"+nrDostawcy+"' and ordernummer = '"+nrZamDost+"'");
							while(nrZamowienia2.next()){
								nrZamowienia = nrZamowienia2.getString("bestelbon");
								dataDost = nrZamowienia2.getString("leverdatum");
							}
							wybierzNrZamowienia.close();
							zamowiono = zamowienie.getString("besteld");
							dostar = zamowienie.getString("geleverd");
							jednostka = zamowienie.getString("besteleenheid");
							dataZam = zamowienie.getString("besteldatum");
						}
						j++;
					}
					wybierzZamowienie.close();
					String typArtykuluSACA = "M";
					if(nrZam.startsWith("1190031")){
						typArtykuluSACA="K";
					}
					else{
						Statement sprawdzTyp = myConn.createStatement();
						
						String sql1 = "SELECT nrZamowienia from Spawane " +
								"where Projekt = '"+calyNumer+"' and kodArt = '"+ArticleNo.replace("'", "")+"' and nrZamowienia = '"+nrZam+"'";
						System.out.println(sql1);
						ResultSet wynik = sprawdzTyp.executeQuery(sql1);
						while(wynik.next()){
							typArtykuluSACA="S";
						}
						sprawdzTyp.close();
					}
					
					if(typArtykuluSACA.equals("K")){
						if(!headerK){
							addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, tableK);
							headerK=true;
						}					
						if(!dataDost.equals(""))
						{
							addCell(tableK, nrZamowienia);
							if(dataZam.length()>10)
								addCell(tableK, dataZam.substring(0, 11));
							else
								addCell(tableK, dataZam);
							if(dataDost.length()>10)
								addCell(tableK, dataDost.substring(0, 11));
							else
								addCell(tableK, dataDost);
							addCell(tableK, nrZam);
							addCell(tableK, ArticleNo);
							addCell(tableK, ArticleName);
							addCell(tableK, zamowiono);
							addCell(tableK, ileDoProj);
							addCell(tableK, dostar);
							addCell(tableK, jednostka);
							Statement takeBon = myConn.createStatement();
							ResultSet rs = takeBon.executeQuery("Select leverancier, ordernummer from storenotesdetail " +
									"where projectnummer = '"+zlozNadrzedne+"' and artikelcode = '"+ArticleNo+"' and besteld <> 0");
							int i = 1;
							String bon="";
							while(rs.next()){
								if(i==ile)
									bon=rs.getString("leverancier")+"/"+rs.getString("ordernummer");
								i++;
							}
							takeBon.close();
							addCell(tableK,bon);
							addCell(tableK,typArtykuluSACA);
						}
					}
					else if(typArtykuluSACA.equals("M")){
						if(!headerM){
							addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, tableM);
							headerM=true;
						}
						if(dataDost==null) dataDost = "";
						if(!dataDost.equals(""))
						{
							addCell(tableM, nrZamowienia);
							if(dataZam.length()>10)
								addCell(tableM, dataZam.substring(0, 11));
							else
								addCell(tableM, dataZam);
							if(dataDost.length()>10)
								addCell(tableM, dataDost.substring(0, 11));
							else
								addCell(tableM, dataDost);
							
							addCell(tableM, nrZam);
							addCell(tableM, ArticleNo);
							addCell(tableM, ArticleName);
							addCell(tableM, zamowiono);
							addCell(tableM, ileDoProj);
							addCell(tableM, dostar);
							addCell(tableM, jednostka);
							Statement takeBon = myConn.createStatement();
							ResultSet rs = takeBon.executeQuery("Select leverancier, ordernummer from storenotesdetail " +
									"where projectnummer = '"+zlozNadrzedne+"' and artikelcode = '"+ArticleNo+"' and besteld <> 0");
							int i = 1;
							String bon="";
							while(rs.next()){
								if(i==ile)
									bon=rs.getString("leverancier")+"/"+rs.getString("ordernummer");
								i++;
							}
							takeBon.close();
							addCell(tableM,bon);
							addCell(tableM,typArtykuluSACA);
						}
					}
					
					if(!header){
						addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, table);
						header=true;
					}
					if(!dataDost.equals(""))
					{
						addCell(table, nrZamowienia);
						if(dataZam.length()>10)
							addCell(table, dataZam.substring(0, 11));
						else
							addCell(table, dataZam);
						if(dataDost.length()>10)
							addCell(table, dataDost.substring(0, 11));
						else
							addCell(table, dataDost);
						addCell(table, nrZam);
						addCell(table, ArticleNo);
						addCell(table, ArticleName);
						addCell(table, zamowiono);
						addCell(table, ileDoProj);
						addCell(table, dostar);
						addCell(table, jednostka);
						Statement takeBon = myConn.createStatement();
						ResultSet rs = takeBon.executeQuery("Select leverancier, ordernummer from storenotesdetail " +
								"where projectnummer = '"+zlozNadrzedne+"' and artikelcode = '"+ArticleNo+"' and besteld <> 0");
						int i = 1;
						String bon="";
						while(rs.next()){
							if(i==ile)
								bon=rs.getString("leverancier")+"/"+rs.getString("ordernummer");
							i++;
						}
						takeBon.close();
						addCell(table,bon);
						addCell(table,typArtykuluSACA);
					}
				}
				takeParts.close();
				
				//pobranie dodatkowych zamowien SACA prosto na projekt
				
				String komentarz = ProjectSchedule.getString("Komentarz");
				String [] p = calyNumer.split("/");
				String ProjectGroup = p[0];
				String ProjectNumber = p[1];
				Statement takeParts2 = myConn.createStatement();
				String sql3 = "bestelling.afdeling = '"+ProjectGroup+"' "
						+ "and bestelling.afdelingseq = '"+ProjectNumber+"' ";
				if(komentarz.length()==8){
					String []p2 = komentarz.split("/");
					sql3="(("+sql3+") or (bestelling.afdeling = '"+p2[0]+"' and bestelling.afdelingseq = '"+p2[1]+"')) ";
				}
				String sql2 = "SELECT bestelling.*, bestellingdetail.artikelcode, bestellingdetail.artikelomschrijving, bestellingdetail.besteld, bestellingdetail.besteleenheid, bestellingdetail.geleverd FROM bestelling "
						+ "join bestellingdetail on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer "
						+ "where "+sql3
						+ "and bestellingdetail.bostdeh <> 0 "
						+ "and bestelling.leverancier like '119003%' ";
				ResultSet parts2 = takeParts2.executeQuery(sql2); 
				while(parts2.next()){
					String typArtykuluSACA = "M";
					String nrZam = parts2.getString("leverancierordernummer");
					String dataDost = parts2.getString("leverdatum");
					String nrZamowienia = parts2.getString("bestelbon");
					String dataZam = parts2.getString("besteldatum");
					String ArticleNo = parts2.getString("artikelcode");
					String ArticleName = parts2.getString("artikelomschrijving");
					String zamowiono = parts2.getString("besteld");
					String ileDoProj = parts2.getString("besteld");
					String dostar = parts2.getString("geleverd");
					String jednostka = parts2.getString("besteleenheid");
					
					parts2.getString("leverancierordernummer");
					if(nrZam.startsWith("1190031")){
						typArtykuluSACA="K";
					}
					
					if(typArtykuluSACA.equals("K")){
						if(!headerK){
							addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, tableK);
							headerK=true;
						}					
						if(!dataDost.equals(""))
						{
							addCell(tableK, nrZamowienia);
							if(dataZam.length()>10)
								addCell(tableK, dataZam.substring(0, 11));
							else
								addCell(tableK, dataZam);
							if(dataDost.length()>10)
								addCell(tableK, dataDost.substring(0, 11));
							else
								addCell(tableK, dataDost);
							
							addCell(tableK, nrZam);
							addCell(tableK, ArticleNo);
							addCell(tableK, ArticleName);
							addCell(tableK, zamowiono);
							addCell(tableK, ileDoProj);
							addCell(tableK, dostar);
							addCell(tableK, jednostka);
							addCell(tableK,"-");
							addCell(tableK,typArtykuluSACA);
						}
					}
					else{
						if(!headerM){
							addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, tableM);
							headerM=true;
						}
						if(dataDost==null) dataDost = "";
						if(!dataDost.equals(""))
						{
							addCell(tableM, nrZamowienia);
							if(dataZam.length()>10)
								addCell(tableM, dataZam.substring(0, 11));
							else
								addCell(tableM, dataZam);
							if(dataDost.length()>10)
								addCell(tableM, dataDost.substring(0, 11));
							else
								addCell(tableM, dataDost);
							addCell(tableM, nrZam);
							addCell(tableM, ArticleNo);
							addCell(tableM, ArticleName);
							addCell(tableM, zamowiono);
							addCell(tableM, ileDoProj);
							addCell(tableM, dostar);
							addCell(tableM, jednostka);
							addCell(tableM,"-");
							addCell(tableM,typArtykuluSACA);
						}
					}
					
					if(!header){
						addProjectHeader(calyNumer, ProjectName, ProductionDate, MontageFinishDate, klient, table);
						header=true;
					}
					if(!dataDost.equals(""))
					{
						addCell(table, nrZamowienia);
						if(dataDost.length()>10)
							addCell(table, dataDost.substring(0, 11));
						else
							addCell(table, dataDost);
						if(dataZam.length()>10)
							addCell(table, dataZam.substring(0, 11));
						else
							addCell(table, dataZam);
						addCell(table, nrZam);
						addCell(table, ArticleNo);
						addCell(table, ArticleName);
						addCell(table, zamowiono);
						addCell(table, ileDoProj);
						addCell(table, dostar);
						addCell(table, jednostka);
						addCell(table,"-");
						addCell(table,typArtykuluSACA);
					}
				}
				takeParts2.close();
				
				if(headerM)
					addRow(tableM);
				if(headerK)
					addRow(tableK);
				if(header)
					addRow(table);
				System.out.println();
			}
			tableM.setWidthPercentage(100);
			tableM.setWidths(widths);
			tableM.setHeaderRows(1);
			tableM.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableM.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(tableM.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				ListMech.add(a1);
			}
			else
				ListMech.add(tableM);
			ListMech.close();
			
			tableK.setWidthPercentage(100);
			tableK.setWidths(widths);
			tableK.setHeaderRows(1);
			tableK.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableK.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(tableK.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				ListK.add(a1);
			}
			else
				ListK.add(tableK);
			ListK.close();
			
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHeaderRows(1);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.setHorizontalAlignment(Element.ALIGN_CENTER);	
			if(table.size()==0 ){
				Paragraph a1 = new Paragraph("Document is empty", ffont2);
				List.add(a1);
			}
			else
				List.add(table);
			List.close();
			
			myConn.close();
		}
		catch (FileNotFoundException | DocumentException | SQLException e) {
				e.printStackTrace();
		}
	}
	
	private static void addHeader(PdfPTable t){
		
		//adding header to our file
		PdfPCell cell1 = new PdfPCell(new Phrase("Nr zamowienia", ffont));
		cell1.setMinimumHeight(30);
		cell1.setBackgroundColor(BaseColor.ORANGE);
		cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell1);
		
		PdfPCell cell12 = new PdfPCell(new Phrase("Data zlozenia zamowienia", ffont));
		cell12.setMinimumHeight(30);
		cell12.setBackgroundColor(BaseColor.ORANGE);
		cell12.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell12.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell12);
		
		PdfPCell cell13 = new PdfPCell(new Phrase("Data dostarczenia zamowienia", ffont));
		cell13.setMinimumHeight(30);
		cell13.setBackgroundColor(BaseColor.ORANGE);
		cell13.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell13.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell13);
		
		PdfPCell cell10 = new PdfPCell(new Phrase("Nr zamowienia2", ffont));
		cell10.setMinimumHeight(30);
		cell10.setBackgroundColor(BaseColor.ORANGE);
		cell10.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell10.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell10);
		
		PdfPCell cell2 = new PdfPCell(new Phrase("Kod artykulu ", ffont));
		cell2.setMinimumHeight(30);
		cell2.setBackgroundColor(BaseColor.ORANGE);
		cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell2);
		
		PdfPCell cell3 = new PdfPCell(new Phrase("Nazwa artykulu", ffont));
		cell3.setMinimumHeight(30);
		cell3.setBackgroundColor(BaseColor.ORANGE);
		cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell3);
				
		PdfPCell cell5 = new PdfPCell(new Phrase("Zamowiono", ffont));
		cell5.setMinimumHeight(30);
		cell5.setBackgroundColor(BaseColor.ORANGE);
		cell5.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell5.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell5);
		
		PdfPCell cell8 = new PdfPCell(new Phrase("Do projektu", ffont));
		cell8.setMinimumHeight(30);
		cell8.setBackgroundColor(BaseColor.ORANGE);
		cell8.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell8.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell8);
		
		PdfPCell cell6 = new PdfPCell(new Phrase("Dostarczono", ffont));
		cell6.setMinimumHeight(30);
		cell6.setBackgroundColor(BaseColor.ORANGE);
		cell6.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell6.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell6);

		PdfPCell cell7 = new PdfPCell(new Phrase("Jednostka", ffont));
		cell7.setMinimumHeight(30);
		cell7.setBackgroundColor(BaseColor.ORANGE);
		cell7.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell7.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell7);
		
		PdfPCell cell9 = new PdfPCell(new Phrase("Nr bonu", ffont));
		cell9.setMinimumHeight(30);
		cell9.setBackgroundColor(BaseColor.ORANGE);
		cell9.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell9.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell9);
		
		PdfPCell cell91 = new PdfPCell(new Phrase("Typ", ffont));
		cell91.setMinimumHeight(30);
		cell91.setBackgroundColor(BaseColor.ORANGE);
		cell91.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell91.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell91);
	}
	
	private static void addProjectHeader(String project, String name, String ProductionDate, String MontageFinishDate, String client, PdfPTable t){
		PdfPCell cell1 = new PdfPCell(new Phrase("Numer projektu", ffont));
		cell1.setFixedHeight(15f);
		cell1.setColspan(3);
		cell1.setBackgroundColor(BaseColor.ORANGE);
		cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell1);
		
		PdfPCell cell2 = new PdfPCell(new Phrase("Nazwa projektu", ffont));
		cell2.setFixedHeight(15f);
		cell2.setColspan(2);
		cell2.setBackgroundColor(BaseColor.ORANGE);
		cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell2);
		
		PdfPCell cell3 = new PdfPCell(new Phrase("Data produkcji czêœci", ffont));
		cell3.setFixedHeight(15f);
		cell3.setBackgroundColor(BaseColor.ORANGE);
		cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell3);
		
		PdfPCell cell35 = new PdfPCell(new Phrase("Data koñca monta¿u", ffont));
		cell35.setFixedHeight(15f);
		cell35.setColspan(2);
		cell35.setBackgroundColor(BaseColor.ORANGE);
		cell35.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell35.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell35);
		
		PdfPCell cell4 = new PdfPCell(new Phrase("Klient", ffont));
		cell4.setFixedHeight(15f);
		cell4.setColspan(4);
		cell4.setBackgroundColor(BaseColor.ORANGE);
		cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell4);
		
		PdfPCell cell04 = new PdfPCell(new Phrase(project, ffont));
		cell04.setFixedHeight(15f);
		cell04.setColspan(4);
		cell04.setBackgroundColor(BaseColor.YELLOW);
		cell04.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell04.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell04);
		
		PdfPCell cell5 = new PdfPCell(new Phrase(name, ffont));
		cell5.setFixedHeight(15f);
		cell5.setColspan(2);
		cell5.setBackgroundColor(BaseColor.ORANGE);
		cell5.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell5.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell5);
		
		PdfPCell cell6 = new PdfPCell(new Phrase(ProductionDate, ffont));
		cell6.setFixedHeight(15f);
		cell6.setColspan(2);
		cell6.setBackgroundColor(BaseColor.ORANGE);
		cell6.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell6.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell6);
		
		PdfPCell cell65 = new PdfPCell(new Phrase(MontageFinishDate, ffont));
		cell65.setFixedHeight(15f);
		cell65.setColspan(2);
		cell65.setBackgroundColor(BaseColor.ORANGE);
		cell65.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell65.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell65);
		
		PdfPCell cell7 = new PdfPCell(new Phrase(client, ffont));
		cell7.setFixedHeight(15f);
		cell7.setColspan(4);
		cell7.setBackgroundColor(BaseColor.ORANGE);
		cell7.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell7.setVerticalAlignment(Element.ALIGN_MIDDLE);
		t.addCell(cell7);
	}

	private static void addRow(PdfPTable t){
		PdfPCell cell = new PdfPCell(new Phrase(" ",ffont2));
		cell.setNoWrap(true);
		cell.setColspan(12);
		t.addCell(cell);
		
	}
	
	public static void addCell(PdfPTable t, String z){
		addCell(false, t, z);
	}
	
	public static void addCell(boolean grey, PdfPTable t, String z){
		PdfPCell cell = new PdfPCell(new Phrase(z, ffont3));
		if(grey)
			cell.setBackgroundColor(new BaseColor(200, 200, 200));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setFixedHeight(15f);
		t.addCell(cell);
	}
}
