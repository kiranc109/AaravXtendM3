/****************************************************************************************
 Extension Name: EXT003MI/GetVanwykIn
 Type: ExtendM3Transaction
 Script Author: Abhijit.Patil@harman.com
 Date: 2026-03-25
 Description:
 * Get records from EXTVWI table

 Revision History:
 Name                         Date         Version   Description of Changes
 Abhijit.Patil@harman.com     2026-03-25   1.0       Initial version - Get records from EXTVWI table
*****************************************************************************************/

public class GetVanwykIn extends ExtendM3Transaction {
  private final MIAPI mi;
  private final DatabaseAPI database;
  private final ProgramAPI program;

  private String inMfno, inType, inIngr;
  private int inCono, inDlix, inPlsx, inSqor;

  public GetVanwykIn(MIAPI mi, DatabaseAPI database, ProgramAPI program) {
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
    getRecord();
  }

  /**
   * Get records from EXTVWI table
   * @params
   * @return
   */
  public void getRecord() {
    DBAction query = database.table("EXTVWI").index("00").selection("EXCONO", "EXTYPE", "EXMFNO", "EXDLIX", "EXPLSX", "EXSQOR", "EXINGR", "EXINNM", "EXWHSL", "EXTRQT", "EXDSQT", "EXBANO", "EXUNMS", "EXBQTY", "EXRGDT", "EXRGTM", "EXLMDT", "EXCHNO", "EXCHID").build();
    DBContainer container = query.getContainer();

    container.set("EXCONO", inCono);
    container.set("EXTYPE", inType);
    container.set("EXMFNO", inMfno);
    container.set("EXDLIX", inDlix);
    container.set("EXPLSX", inPlsx);
    container.set("EXSQOR", inSqor);
    container.set("EXINGR", inIngr);

    if (query.read(container)) {
      mi.outData.put("CONO", container.get("EXCONO").toString());
      mi.outData.put("TYPE", container.get("EXTYPE").toString());
      mi.outData.put("MFNO", container.get("EXMFNO").toString());
      mi.outData.put("DLIX", container.get("EXDLIX").toString());
      mi.outData.put("PLSX", container.get("EXPLSX").toString());
      mi.outData.put("SQOR", container.get("EXSQOR").toString());
      mi.outData.put("INGR", container.get("EXINGR").toString());
      mi.outData.put("INNM", container.get("EXINNM").toString());
      mi.outData.put("WHSL", container.get("EXWHSL").toString());
      mi.outData.put("TRQT", container.get("EXTRQT").toString());
      mi.outData.put("DSQT", container.get("EXDSQT").toString());
      mi.outData.put("BANO", container.get("EXBANO").toString());
      mi.outData.put("UNMS", container.get("EXUNMS").toString());
      mi.outData.put("BQTY", container.get("EXBQTY").toString());
      mi.outData.put("RGDT", container.get("EXRGDT").toString());
      mi.outData.put("RGTM", container.get("EXRGTM").toString());
      mi.outData.put("LMDT", container.get("EXLMDT").toString());
      mi.outData.put("CHNO", container.get("EXCHNO").toString());
      mi.outData.put("CHID", container.get("EXCHID").toString());
      mi.write();
    } else {
      mi.error("Record does not Exist.");
      return;
    }
  }
}
