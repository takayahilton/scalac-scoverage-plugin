package scoverage.report

import scoverage._
import scala.xml.Node
import scoverage.MeasuredFile
import java.util.Date
import java.io.File
import org.apache.commons.io.{FilenameUtils, FileUtils}

/** @author Stephen Samuel */
object ScoverageHtmlWriter extends CoverageWriter {

  def write(coverage: Coverage, dir: File): Unit = {
    val indexFile = new File(dir.getAbsolutePath + "/index.html")
    val packageFile = new File(dir.getAbsolutePath + "/packages.html")
    val overviewFile = new File(dir.getAbsolutePath + "/overview.html")

    FileUtils.copyInputStreamToFile(getClass.getResourceAsStream("/index.html"), indexFile)
    FileUtils.write(packageFile, packages(coverage).toString())
    FileUtils.write(overviewFile, overview(coverage).toString())

    coverage.packages.foreach(write(_, dir))
  }

  def write(pack: MeasuredPackage, dir: File) {
    val file = new File(dir.getAbsolutePath + "/" + pack.name.replace('.', '/') + "/package.html")
    file.getParentFile.mkdirs()
    FileUtils.write(file, classes(pack).toString())
    pack.files.foreach(write(_, file.getParentFile))
  }

  def write(mfile: MeasuredFile, dir: File) {
    val file = new File(dir.getAbsolutePath + "/" + FilenameUtils.getBaseName(mfile.source) + ".html")
    file.getParentFile.mkdirs()
    FileUtils.write(file, _file(mfile).toString())
  }

  def _file(mfile: MeasuredFile): Node = {
    val css =
      "table.codegrid { font-family: monospace; font-size: 12px; width: auto!important; }" +
        "table.statementlist { width: auto!important; font-size: 13px; } " +
        "table.codegrid td { padding: 0!important; border: 0!important } " +
        "table td.linenumber { width: 40px!important; } "
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>
          {mfile.source}
        </title>
        <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"/>
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
        <script src="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
        <style>
          {css}
        </style>
      </head>
      <body style="font-family: monospace;">
        <ul class="nav nav-tabs">
          <li>
            <a href="#codegrid" data-toggle="tab">Codegrid</a>
          </li>
          <li>
            <a href="#statementlist" data-toggle="tab">Statement List</a>
          </li>
        </ul>
        <div class="tab-content">
          <div class="tab-pane active" id="codegrid">
            {new CodeGrid(mfile).output}
          </div>
          <div class="tab-pane" id="statementlist">
            {new StatementWriter(mfile).output}
          </div>
        </div>
      </body>
    </html>

  }

  def classes(pack: MeasuredPackage): Node = {
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>Scales Code Coverage</title>
        <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"/>
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
        <script src="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
      </head>
      <body style="font-family: monospace;">
        <table class="table table-striped" style="font-size:13px">
          <thead>
            <tr>
              <th>Class</th>
              <th>Source file</th>
              <th>Lines</th>
              <th>Methods</th>
              <th>Statements</th>
              <th>Stmt Invoked</th>
              <th>Stmt Coverage</th>
              <th>Branches</th>
              <th>Br Invoked</th>
              <th>Br Coverage</th>
            </tr>
          </thead>
          <tbody>
            {pack.classes.map(_class)}
          </tbody>
        </table>
      </body>
    </html>
  }

  def _class(klass: MeasuredClass): Node = {
    val filename = FilenameUtils.getBaseName(klass.source) + ".html"
    val simpleClassName = klass.name.split('.').last
    <tr>
      <td>
        <a href={filename}>
          {simpleClassName}
        </a>
      </td>
      <td>
        {klass.statements.headOption.map(_.source.split('/').last).getOrElse("")}
      </td>
      <td>
        {klass.loc.toString}
      </td>
      <td>
        {klass.methodCount.toString}
      </td>
      <td>
        {klass.statementCount.toString}
      </td>
      <td>
        {klass.invokedStatementCount.toString}
      </td>
      <td>
        {klass.statementCoverageFormatted}
        %
      </td>
      <td>
        {klass.branchCount.toString}
      </td>
      <td>
        {klass.invokedBranchesCount.toString}
      </td>
      <td>
        {klass.branchCoverageFormatted}
        %
      </td>
    </tr>
  }

  def packages(coverage: Coverage): Node = {
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>Scales Code Coverage</title>
        <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"/>
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
        <script src="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
      </head>
      <body style="font-family: monospace;">
        <table class="table table-striped" style="font-size: 13px">
          <tbody>
            <tr>
              <td>
                <a href="overview.html" target="mainFrame">
                  All packages
                </a>{coverage.statementCoverageFormatted}
                %
              </td>
            </tr>{coverage.packages.map(arg =>
            <tr>
              <td>
                <a href={arg.name.replace('.', '/') + "/package.html"} target="mainFrame">
                  {arg.name}
                </a>{arg.statementCoverageFormatted}
                %
              </td>
            </tr>
          )}
          </tbody>
        </table>
      </body>
    </html>
  }

  def risks(coverage: Coverage, limit: Int) = {
    <table class="table table-striped" style="font-size: 12px">
      <caption>Top 20 Class Risks</caption>
      <thead>
        <tr>
          <th>Class</th>
          <th>Lines</th>
          <th>Methods</th>
          <th>Statements</th>
          <th>Statement Rate</th>
          <th>Branches</th>
          <th>Branch Rate</th>
        </tr>
      </thead>
      <tbody>
        {coverage.risks(limit).map(klass =>
        <tr>
          <td>
            {klass.simpleName}
          </td>
          <td>
            {klass.loc.toString}
          </td>
          <td>
            {klass.methodCount.toString}
          </td>
          <td>
            {klass.statementCount.toString}
          </td>
          <td>
            {klass.statementCoverageFormatted}
            %
          </td>
          <td>
            {klass.branchCount.toString}
          </td>
          <td>
            {klass.branchCoverageFormatted}
            %
          </td>
        </tr>)}
      </tbody>
    </table>
  }

  def packages2(coverage: Coverage) = {
    val rows = coverage.packages.map(arg => {
      <tr>
        <td>
          {arg.name}
        </td>
        <td>
          {arg.invokedClasses.toString}
          /
          {arg.classCount}
          (
          {arg.classCoverage.toString}
          %)
        </td>
        <td>
          {arg.invokedStatements.toString()}
          /
          {arg.statementCount}
          (
          {arg.statementCoverageFormatted}
          %)
        </td>
      </tr>
    })
    <table>
      {rows}
    </table>
  }

  def overview(coverage: Coverage): Node = {
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title id='title'>Scales Code Coverage</title>
        <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"/>
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
        <script src="http://netdna.bootstrapcdn.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>
      </head>
      <body>
        <div class="overview">
          {stats(coverage)}{risks(coverage, 20)}
        </div>
      </body>
    </html>
  }

  def stats(coverage: Coverage): Node = {
    <table class="table">
      <caption>Statistics generated at
        {new Date().toString}
      </caption>
      <tr>
        <td>Lines of code:</td>
        <td>
          {coverage.loc.toString}
        </td>
        <td>Statements:</td>
        <td>
          {coverage.statementCount}
        </td>
        <td>Clases per package:</td>
        <td>
          {coverage.avgClassesPerPackageFormatted}
        </td>
        <td>Methods per class:</td>
        <td>
          {coverage.avgMethodsPerClassFormatted}
        </td>
      </tr>
      <tr>
        <td>to be completed</td>
        <td>
        </td>
        <td>Packages:</td>
        <td>
          {coverage.classCount.toString}
        </td>
        <td>Classes:</td>
        <td>
          {coverage.packageCount.toString}
        </td>
        <td>Methods:</td>
        <td>
          {coverage.methodCount.toString}
        </td>
      </tr>
    </table>
  }

}

