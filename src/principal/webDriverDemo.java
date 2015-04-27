package principal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;

public class webDriverDemo {

  public static void main(String[] args) throws Exception {
	
	WebDriver driver;
	String baseUrl;
    driver = new FirefoxDriver();
    baseUrl = "http://www.skyscanner.it";
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    
    driver.get(baseUrl + "/");
    
    //Inserisco aeroporto di partenza
    String origin = "Milano Malpensa (MXP)";
    driver.findElement(By.id("js-origin-input")).clear();
    driver.findElement(By.id("js-origin-input")).sendKeys(origin);
    driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
    
    //Inserisco aeroporto di ritorno
    String destination = "Parigi Charles de Gaulle (CDG)";
    driver.findElement(By.id("js-destination-input")).clear();
    driver.findElement(By.id("js-destination-input")).sendKeys(destination);
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    
/*	METODO 1: in teoria volevo selezionare i giorno della partenza e del ritorno 
 * cercando tutti gli elementi che contengono i "numerini" della partenza e del ritorno
 * -NON FUNZIONA-

    //Inserisco data di partenza
    String departureDay = "8";
    driver.findElement(By.id("js-depart-input")).click();
    List<WebElement> days = driver.findElements(By.linkText(departureDay));
    System.out.println("ANDATA: Numero di elementi che corrispondo al giorno che volgiamo selezionare: " + days.size());
    
    //driver.findElement(By.linkText(departureDay)).click();
    
    //Inserisco data di ritorno
    String returnDay = "24";
    driver.findElement(By.id("js-return-input")).click();
    days = driver.findElements(By.xpath("(//a[contains(text(),returnDay)])[*]"));
    System.out.println("RITORNO: Numero di elementi che corrispondo al giorno che volgiamo selezionare: " + days.size());
*/

	//METODO 2
    
    //PROBLEMA: io cerco tutti i <td> e dentro cerco il giorno della mia partenza
    //Ma se voglio partire il 30, rischio di selezionare il 30 del mese precedente
    
    //Seleziono l'input text dell'andata per far apparire il calendario
	driver.findElement(By.id("js-depart-input")).click();  
	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);  
	//Potrei selezionare 'next' per andare al mese successivo (non so se funziona)
	//driver.findElement(By.xpath("/html/body/div[9]/div/div[2]/div[1]/a[2]")).click();  
      
	//Il calendario che appare è una tabella, navigo nella tabella cercando il giorno in cui voglio partire  
	WebElement dateWidgetDepart = driver.findElement(By.className("container-body"));  
	List<WebElement> columnsDeparture = dateWidgetDepart.findElements(By.tagName("td"));  
	
	int i = 0;
	//Giusto per una mia curiosità stampo il contenuto di tutte le colonne
	for (WebElement cell : columnsDeparture) {
		i++;
		System.out.println(i  + ": " + cell.getText());
	}
	
	String departureDay = "29";
	
	for (WebElement cell : columnsDeparture) {  
		if (cell.getText().equals(departureDay)) {
			cell.findElement(By.linkText(departureDay)).click();  
			break;
		}
	}
	
	Thread.sleep(2500);
	
	//Seleziono allo stesso modo la data della partenza
	driver.findElement(By.id("js-return-input")).click();  
	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);  
	//Seleziono 'next' per cambiare mese
	//driver.findElement(By.xpath("//*[@id=\"category-flights\"]/div[9]/div/div[2]/div[1]/a[2]")).click();  
	      
	//PROBLEMA: Qui non so perchè non mi salva nessun dato: quando visualizzo tutte le celle
	//mi risultano tutte vuote
	WebElement dateWidgetReturn = driver.findElement(By.className("container-body"));
	List<WebElement> columnsReturn = dateWidgetReturn.findElements(By.tagName("td"));  
	
	i = 0;
	for (WebElement cell : columnsReturn) {
		i++;
		System.out.println(i  + ": " + cell.getText());
	}
	
	String returnDay = "29";
	
	for (WebElement cell : columnsReturn) {    
		if (cell.getText().equals(returnDay)) {
			cell.findElement(By.linkText(returnDay)).click();  
			break;
		}
	}

	//METODO 3
	//Setto la data usando javascript "23/07/2015
	//PROVLEMA: mi cambia il valore visulaizzato dall'input text ma non va realmente a modificare il dato nella ricerca
	
//	JavascriptExecutor js = (JavascriptExecutor) driver;
//	js.executeScript("document.querySelectorAll('#js-depart-input')[0].setAttribute('value', '23/07/2015')");
//	driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//	js.executeScript("document.querySelectorAll('#js-return-input')[0].setAttribute('value', '24/07/2015')");
//	driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
   
    //Avvio la ricerca
    driver.findElement(By.id("js-search-button")).click();
    
    System.out.println("La ricerca ha portato alla pagina: " + driver.getCurrentUrl());
    
    driver.quit();
  }
}
