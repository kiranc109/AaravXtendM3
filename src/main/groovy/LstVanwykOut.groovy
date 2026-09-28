/****************************************************************************************
 Extension Name: EXT002MI/LstVanwykOut
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-29
 Description:
 * List records from EXTVWO table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-29   1.0       Initial version - List records from EXTVWO table
*****************************************************************************************/

public class LstVanwykOut extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;

  private String inMfno, inType, inIngr;
  private int inCono, inDlix, inPlsx, inSqor;

  public LstVanwykOut(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
    this.mi = mi;
    this.database = database;
    this.program = program;
  }

  /**
   * Main function
   * @param
   * @return
   */
  public void main() {
    inCono = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? program.LDAZD.CONO as Integer : mi.inData.get("CONO") as Integer;
    inType = (mi.inData.get("TYPE") == null || mi.inData.get("TYPE").trim().isEmpty()) ? "" : mi.inData.get("TYPE");
    inMfno = (mi.inData.get("MFNO") == null || mi.inData.get("MFNO").trim().isEmpty()) ? "" : mi.inData.get("MFNO");
    inDlix = (mi.inData.get("DLIX") == null || mi.inData.get("DLIX").trim().isEmpty()) ? 0 : mi.inData.get("DLIX") as Integer;
    inPlsx = (mi.inData.get("PLSX") == null || mi.inData.get("PLSX").trim().isEmpty()) ? 0 : mi.inData.get("PLSX") as Integer;
    inSqor = (mi.inData.get("SQOR") == null || mi.inData.get("SQOR").trim().isEmpty()) ? 0 : mi.inData.get("SQOR") as Integer;
    inIngr = (mi.inData.get("INGR") == null || mi.inData.get("INGR").trim().isEmpty()) ? "" : mi.inData.get("INGR");
    listRecord();
  }

  /**
   * List records from EXTVWO table
   * @params
   * @return
   */
  public void listRecord() {
    DBAction query = database.table("EXTVWO").index("00").selection("EXCONO", "EXTYPE", "EXMFNO", "EXDLIX", "EXPLSX", "EXSQOR", "EXINGR", "EXTRQT", "EXTQTY", "EXRGDT", "EXRGTM", "EXLMDT", "EXCHNO", "EXCHID").build();
    DBContainer container = query.getContainer();

    container.set("EXCONO", inCono);
    container.set("EXTYPE", inType);
    container.set("EXMFNO", inMfno);
    container.set("EXDLIX", inDlix);
    container.set("EXPLSX", inPlsx);
    container.set("EXSQOR", inSqor);
    container.set("EXINGR", inIngr);

    int nrOfRecords = mi.getMaxRecords() <= 0 || mi.getMaxRecords() >= 10000 ? 10000 : mi.getMaxRecords();

    Closure<?> resultset = {
      DBContainer result ->
        mi.outData.put("CONO", result.get("EXCONO").toString());
        mi.outData.put("TYPE", result.get("EXTYPE").toString());
        mi.outData.put("MFNO", result.get("EXMFNO").toString());
        mi.outData.put("DLIX", result.get("EXDLIX").toString());
        mi.outData.put("PLSX", result.get("EXPLSX").toString());
        mi.outData.put("SQOR", result.get("EXSQOR").toString());
        mi.outData.put("INGR", result.get("EXINGR").toString());
        mi.outData.put("TRQT", result.get("EXTRQT").toString());
        mi.outData.put("TQTY", result.get("EXTQTY").toString());
        mi.outData.put("RGDT", result.get("EXRGDT").toString());
        mi.outData.put("RGTM", result.get("EXRGTM").toString());
        mi.outData.put("LMDT", result.get("EXLMDT").toString());
        mi.outData.put("CHNO", result.get("EXCHNO").toString());
        mi.outData.put("CHID", result.get("EXCHID").toString());
        mi.write();
    }

    if (inCono != 0 && !inType.isEmpty() && !inMfno.isEmpty() && inDlix != 0 && inPlsx != 0 && inSqor != 0 && !inIngr.isEmpty()) {
      query.readAll(container, 7, nrOfRecords, resultset);
    } else if (inCono != 0 && !inType.isEmpty() && !inMfno.isEmpty() && inDlix != 0 && inPlsx != 0 && inSqor != 0) {
      query.readAll(container, 6, nrOfRecords, resultset);
    } else if (inCono != 0 && !inType.isEmpty() && !inMfno.isEmpty() && inDlix != 0 && inPlsx != 0) {
      query.readAll(container, 5, nrOfRecords, resultset);
    } else if (inCono != 0 && !inType.isEmpty() && !inMfno.isEmpty() && inDlix != 0) {
      query.readAll(container, 4, nrOfRecords, resultset);
    } else if (inCono != 0 && !inType.isEmpty() && !inMfno.isEmpty()) {
      query.readAll(container, 3, nrOfRecords, resultset);
    } else if (inCono != 0 && !inType.isEmpty()) {
      query.readAll(container, 2, nrOfRecords, resultset);
    } else if (inCono != 0) {
      query.readAll(container, 1, nrOfRecords, resultset);
    }
  }
}
